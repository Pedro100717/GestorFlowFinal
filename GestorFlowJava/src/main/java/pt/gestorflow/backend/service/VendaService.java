package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.VendaDTO;
import pt.gestorflow.backend.dto.VendaResponseDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendaService {

    private final VendaRepository vendaRepository;
    private final ClienteRepository clienteRepository;
    private final ArtigoRepository artigoRepository;
    private final TxIvaRepository txIvaRepository;
    private final CentroCustoRepository centroCustoRepository;
    private final SeccaoHomoRepository seccaoHomoRepository;

    // --- INJEÇÕES DA TESOURARIA ---
    private final ContaBancariaRepository contaRepository;
    private final MovimentoRepository movimentoRepository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Transactional
    public VendaResponseDTO registarVenda(VendaDTO dto) {
        Utilizador user = getUtilizadorLogado();

        // 1. Buscar Entidades
        Cliente cliente = clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));

        Artigo artigo = artigoRepository.findById(dto.getArtigoId())
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado"));

        TxIva taxaIva = txIvaRepository.findById(dto.getTaxaIvaId())
                .orElseThrow(() -> new EntityNotFoundException("Taxa de IVA não encontrada"));

        // 2. Lógica de Stock (Apenas se for Mercadoria)
        if (artigo instanceof Mercadoria mercadoria) {
            // Deduzir stock
            mercadoria.setStockAtual(mercadoria.getStockAtual().subtract(dto.getQuantidade()));
            artigoRepository.save(mercadoria);
        }

        // 3. Criar Objeto Venda
        Venda venda = new Venda();
        if (dto.getDataVenda() != null) {
            venda.setDataVenda(dto.getDataVenda().atStartOfDay());
        } else {
            venda.setDataVenda(LocalDateTime.now());
        }
        venda.setCliente(cliente);
        venda.setArtigo(artigo);
        venda.setUtilizador(user);
        venda.setTaxaIva(taxaIva);

        // Designação
        if (dto.getDesignacaoPersonalizada() != null && !dto.getDesignacaoPersonalizada().isBlank()) {
            venda.setDesignacao(dto.getDesignacaoPersonalizada());
        } else {
            venda.setDesignacao(artigo.getNome());
        }

        // 4. Cálculos Financeiros
        venda.setQuantidade(dto.getQuantidade());
        venda.setPrecoUnitario(dto.getPrecoUnitario());

        BigDecimal totalSemIva = dto.getPrecoUnitario().multiply(dto.getQuantidade());
        BigDecimal percentagemIva = taxaIva.getValor().divide(BigDecimal.valueOf(100));
        BigDecimal valorIva = totalSemIva.multiply(percentagemIva);
        BigDecimal totalComIva = totalSemIva.add(valorIva);

        venda.setTotalSemIva(totalSemIva);
        venda.setTotalComIva(totalComIva);

        // 5. Analítica
        if (dto.getCentroCustoId() != null) {
            centroCustoRepository.findById(dto.getCentroCustoId()).ifPresent(venda::setCentroCusto);
        }
        if (dto.getSeccaoHomoId() != null) {
            seccaoHomoRepository.findById(dto.getSeccaoHomoId()).ifPresent(venda::setSeccaoHomo);
        }

        // 6. Guardar a venda primeiro para gerar o ID
        Venda vendaGuardada = vendaRepository.save(venda);

        // ==========================================
        // 7. LIGAÇÃO À TESOURARIA: RECEBIMENTO AUTOMÁTICO
        // ==========================================
        ContaBancaria conta = contaRepository.findById(dto.getContaBancariaId())
                .orElseThrow(() -> new EntityNotFoundException("Conta bancária não encontrada"));

        vendaGuardada.setContaBancaria(conta);
        vendaRepository.save(vendaGuardada);

        // Segurança
        if (!conta.getUtilizador().getId().equals(user.getId())) {
            throw new RuntimeException("Sem permissão para movimentar esta conta.");
        }

        // Adicionar o dinheiro à conta (Crédito)
        conta.setSaldo(conta.getSaldo().add(totalComIva));
        contaRepository.save(conta);

        // Registar o rasto no extrato bancário
        Movimento mov = new Movimento();
        mov.setConta(conta);
        mov.setUtilizador(user);
        mov.setTipo(Movimento.TipoMovimento.CREDITO);
        mov.setValor(totalComIva);
        mov.setSaldoApos(conta.getSaldo());

        mov.setDescricao("Recebimento de Venda - " + cliente.getNome());

        // Ligações vitais ao Cliente e à Venda
        mov.setVenda(vendaGuardada);
        mov.setCliente(cliente);

        movimentoRepository.save(mov);

        // Devolver o DTO Plano limpo e sem ciclos infinitos!
        return converterParaDTO(vendaGuardada);
    }

    public Page<VendaResponseDTO> listarMinhasVendas(int pagina, int tamanho) {
        Utilizador user = getUtilizadorLogado();
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("dataVenda").descending());
        // A magia acontece aqui: converte cada Entidade do repositório no DTO de Saída
        return vendaRepository.findAllByUtilizadorId(user.getId(), pageable).map(this::converterParaDTO);
    }

    public List<TxIva> listarTaxasIva(){
        return txIvaRepository.findAll();
    }

    // ==========================================
    // CONVERSOR: Entidade -> Flat DTO
    // ==========================================
    private VendaResponseDTO converterParaDTO(Venda v) {
        VendaResponseDTO dto = new VendaResponseDTO();
        dto.setId(v.getId());
        dto.setDataVenda(v.getDataVenda());
        dto.setDesignacao(v.getDesignacao());
        dto.setQuantidade(v.getQuantidade());
        dto.setPrecoUnitario(v.getPrecoUnitario());
        dto.setTotalSemIva(v.getTotalSemIva());
        dto.setTotalComIva(v.getTotalComIva());

        if (v.getCliente() != null) {
            dto.setClienteId(v.getCliente().getId());
            dto.setClienteNome(v.getCliente().getNome());
        }
        if (v.getArtigo() != null) {
            dto.setArtigoId(v.getArtigo().getId());
            dto.setArtigoNome(v.getArtigo().getNome());
        }
        if (v.getCentroCusto() != null) {
            dto.setCentroCustoId(v.getCentroCusto().getId());
            dto.setCentroCustoCodigo(v.getCentroCusto().getCodigo());
        }
        if (v.getSeccaoHomo() != null) {
            dto.setSeccaoHomoId(v.getSeccaoHomo().getId());
            dto.setSeccaoHomoCodigo(v.getSeccaoHomo().getCodigo());
        }
        if (v.getTaxaIva() != null) {
            dto.setTaxaIvaId(v.getTaxaIva().getId());
            dto.setTaxaIvaValor(v.getTaxaIva().getValor());
        }
        if (v.getContaBancaria() != null) {
            dto.setContaBancariaId(v.getContaBancaria().getId());
            dto.setContaBancariaNome(v.getContaBancaria().getNome());
        }
        return dto;
    }
}