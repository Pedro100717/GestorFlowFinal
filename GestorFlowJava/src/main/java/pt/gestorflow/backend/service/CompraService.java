package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.CompraDTO;
import pt.gestorflow.backend.dto.CompraResponseDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompraService {

    private final CompraRepository compraRepository;
    private final FornecedorRepository fornecedorRepository;
    private final ArtigoRepository artigoRepository;
    private final CentroCustoRepository centroCustoRepository;
    private final SeccaoHomoRepository seccaoHomoRepository;
    private final TxIvaRepository txIvaRepository;

    // --- INJEÇÕES DA TESOURARIA ---
    private final ContaBancariaRepository contaRepository;
    private final MovimentoRepository movimentoRepository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Transactional
    public CompraResponseDTO registarCompra(CompraDTO dto) {
        Utilizador user = getUtilizadorLogado();

        Fornecedor fornecedor = fornecedorRepository.findById(dto.getFornecedorId())
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado"));

        Artigo artigo = artigoRepository.findById(dto.getArtigoId())
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado"));

        // 1. Buscar Taxa de IVA (Obrigatória na compra)
        TxIva taxaIva = txIvaRepository.findById(dto.getTaxaIvaId())
                .orElseThrow(() -> new EntityNotFoundException("Taxa de IVA não encontrada"));

        // 2. Lógica de Stock e Custo
        // Verifica se é Mercadoria antes de mexer no stock!
        if (artigo instanceof Mercadoria mercadoria) {
            mercadoria.setStockAtual(mercadoria.getStockAtual().add(dto.getQuantidade()));
            // O save no final atualiza tudo
        }

        // Atualiza o preço de custo no Pai (Artigo), seja Mercadoria ou Serviço
        artigo.setUltimoPrecoCusto(dto.getPrecoUnitario());
        artigoRepository.save(artigo);

        // 3. Criar Registo da Compra
        Compra compra = new Compra();
        if (dto.getDataCompra() != null) {
            compra.setDataCompra(dto.getDataCompra().atStartOfDay());
        } else {
            compra.setDataCompra(LocalDateTime.now());
        }
        compra.setFornecedor(fornecedor);
        compra.setArtigo(artigo);
        compra.setUtilizador(user);
        compra.setTaxaIva(taxaIva);

        compra.setQuantidade(dto.getQuantidade());
        compra.setPrecoUnitario(dto.getPrecoUnitario());

        // Cálculo do Total (Base * Qtd * (1 + Taxa))
        BigDecimal totalSemIva = dto.getQuantidade().multiply(dto.getPrecoUnitario());
        BigDecimal fatorIva = taxaIva.getValor().divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE);
        BigDecimal totalComIva = totalSemIva.multiply(fatorIva);

        compra.setTotal(totalComIva);
        compra.setNumeroFaturaFornecedor(dto.getNumeroFaturaFornecedor());

        if (dto.getDesignacaoPersonalizada() != null && !dto.getDesignacaoPersonalizada().isBlank()) {
            compra.setDesignacao(dto.getDesignacaoPersonalizada());
        } else {
            compra.setDesignacao(artigo.getNome());
        }

        // Analítica
        if (dto.getCentroCustoId() != null) {
            centroCustoRepository.findById(dto.getCentroCustoId()).ifPresent(compra::setCentroCusto);
        }
        if (dto.getSeccaoHomoId() != null) {
            seccaoHomoRepository.findById(dto.getSeccaoHomoId()).ifPresent(compra::setSeccaoHomo);
        }

        // Guardar a compra primeiro para gerar o ID na base de dados
        Compra compraGuardada = compraRepository.save(compra);

        // ==========================================
        // 4. LIGAÇÃO À TESOURARIA: PAGAMENTO AUTOMÁTICO
        // ==========================================
        ContaBancaria conta = contaRepository.findById(dto.getContaBancariaId())
                .orElseThrow(() -> new EntityNotFoundException("Conta bancária não encontrada"));

        compraGuardada.setContaBancaria(conta);
        compraRepository.save(compraGuardada);

        // Segurança: Garantir que a conta é do utilizador logado
        if (!conta.getUtilizador().getId().equals(user.getId())) {
            throw new RuntimeException("Sem permissão para movimentar esta conta.");
        }

        // Tirar o dinheiro da conta (Débito)
        conta.setSaldo(conta.getSaldo().subtract(totalComIva));
        contaRepository.save(conta);

        // Registar o rasto no extrato bancário
        Movimento mov = new Movimento();
        mov.setConta(conta);
        mov.setUtilizador(user);
        mov.setTipo(Movimento.TipoMovimento.DEBITO);
        mov.setValor(totalComIva);
        mov.setSaldoApos(conta.getSaldo());

        // Construir a descrição do movimento
        String refFatura = dto.getNumeroFaturaFornecedor() != null && !dto.getNumeroFaturaFornecedor().isBlank()
                ? " | Fatura: " + dto.getNumeroFaturaFornecedor()
                : "";
        mov.setDescricao("Pagamento de Compra a " + fornecedor.getNome() + refFatura);

        // Ligações vitais ao Fornecedor e à Compra
        mov.setCompra(compraGuardada);
        mov.setFornecedor(fornecedor);

        movimentoRepository.save(mov);

        // Devolver o DTO Plano limpo e sem ciclos infinitos!
        return converterParaDTO(compraGuardada);
    }

    @Transactional(readOnly = true)
    public Page<CompraResponseDTO> listarMinhasCompras(int pagina, int tamanho) {
        Utilizador user = getUtilizadorLogado();
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("dataCompra").descending());
        // A magia acontece aqui: converte cada Entidade do repositório no DTO de Saída
        return compraRepository.findAllByUtilizadorId(user.getId(), pageable).map(this::converterParaDTO);
    }

    public List<TxIva> listarTaxasIva() {
        return txIvaRepository.findAll();
    }

    // ==========================================
    // CONVERSOR: Entidade -> Flat DTO
    // ==========================================
    private CompraResponseDTO converterParaDTO(Compra c) {
        CompraResponseDTO dto = new CompraResponseDTO();
        dto.setId(c.getId());
        dto.setDataCompra(c.getDataCompra());
        dto.setNumeroFaturaFornecedor(c.getNumeroFaturaFornecedor());
        dto.setDesignacao(c.getDesignacao());
        dto.setQuantidade(c.getQuantidade());
        dto.setPrecoUnitario(c.getPrecoUnitario());
        dto.setTotal(c.getTotal());

        if (c.getFornecedor() != null) {
            dto.setFornecedorId(c.getFornecedor().getId());
            dto.setFornecedorNome(c.getFornecedor().getNome());
        }
        if (c.getArtigo() != null) {
            dto.setArtigoId(c.getArtigo().getId());
            dto.setArtigoNome(c.getArtigo().getNome());
        }
        if (c.getCentroCusto() != null) {
            dto.setCentroCustoId(c.getCentroCusto().getId());
            dto.setCentroCustoCodigo(c.getCentroCusto().getCodigo());
        }
        if (c.getSeccaoHomo() != null) {
            dto.setSeccaoHomoId(c.getSeccaoHomo().getId());
            dto.setSeccaoHomoCodigo(c.getSeccaoHomo().getCodigo());
        }
        if (c.getTaxaIva() != null) {
            dto.setTaxaIvaId(c.getTaxaIva().getId());
            dto.setTaxaIvaValor(c.getTaxaIva().getValor());
        }
        if (c.getContaBancaria() != null) {
            dto.setContaBancariaId(c.getContaBancaria().getId());
            dto.setContaBancariaNome(c.getContaBancaria().getNome());
        }
        return dto;
    }
}