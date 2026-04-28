package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final MovimentoStockRepository movimentoStockRepository;

    // 🚀 Injeções de Segurança e Base de Dados
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    @Transactional
    public CompraResponseDTO registarCompra(CompraDTO dto) {
        // 🚀 1. Buscar a ID blindada
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🚀 2. Buscar a entidade real do Utilizador para associar à compra e ao stock
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        // 🛡️ Validação Comercial (IDOR Protegido)
        Fornecedor fornecedor = fornecedorRepository.findByIdAndUtilizadorId(dto.getFornecedorId(), utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado ou acesso negado."));

        Artigo artigo = artigoRepository.findByIdAndUtilizadorId(dto.getArtigoId(), utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou acesso negado."));

        TxIva taxaIva = txIvaRepository.findById(dto.getTaxaIvaId())
                .orElseThrow(() -> new EntityNotFoundException("Taxa de IVA não encontrada."));

        if (artigo instanceof Mercadoria mercadoria) {
            BigDecimal stockAtual = mercadoria.getStockAtual() != null ? mercadoria.getStockAtual() : BigDecimal.ZERO;
            BigDecimal custoAtual = artigo.getUltimoPrecoCusto() != null ? artigo.getUltimoPrecoCusto() : BigDecimal.ZERO;
            BigDecimal qtdComprada = dto.getQuantidade();
            BigDecimal precoCompra = dto.getPrecoUnitario();

            // 🛡️ CÁLCULO DO PREÇO DE CUSTO MÉDIO PONDERADO (PCMP)
            BigDecimal valorStockExistente = stockAtual.multiply(custoAtual);
            BigDecimal valorNovaCompra = qtdComprada.multiply(precoCompra);
            BigDecimal novoStockTotal = stockAtual.add(qtdComprada);

            if (novoStockTotal.compareTo(BigDecimal.ZERO) > 0) {
                // Divide o valor total pelo stock total (com 4 casas decimais para não perder precisão nos cêntimos)
                BigDecimal precoMedioPonderado = valorStockExistente.add(valorNovaCompra)
                        .divide(novoStockTotal, 4, java.math.RoundingMode.HALF_UP);

                artigo.setUltimoPrecoCusto(precoMedioPonderado);
            } else {
                artigo.setUltimoPrecoCusto(precoCompra);
            }

            // Atualiza a quantidade do artigo
            mercadoria.setStockAtual(novoStockTotal);
            artigoRepository.save(artigo);

            // Regista o movimento histórico
            MovimentoStock mov = new MovimentoStock();
            mov.setMercadoria(mercadoria);
            mov.setUtilizador(user);
            mov.setTipo(MovimentoStock.TipoMovimentoStock.ENTRADA);
            mov.setQuantidade(qtdComprada);
            mov.setStockAposMovimento(mercadoria.getStockAtual());
            mov.setMotivo("Compra a Fornecedor: " + fornecedor.getNome());
            movimentoStockRepository.save(mov);
        } else {
            // Se for um serviço, apenas atualiza o preço de custo (não há stock para ponderar)
            artigo.setUltimoPrecoCusto(dto.getPrecoUnitario());
            artigoRepository.save(artigo);
        }

        // 2. Calcular Totais
        BigDecimal totalSemIva = dto.getQuantidade().multiply(dto.getPrecoUnitario());
        BigDecimal fatorIva = taxaIva.getValor().divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE);
        BigDecimal totalComIva = totalSemIva.multiply(fatorIva);

        // 3. Criar a Compra (Estado PENDENTE)
        Compra compra = new Compra();
        compra.setDataCompra(dto.getDataCompra() != null ? dto.getDataCompra().atStartOfDay() : LocalDateTime.now());
        compra.setFornecedor(fornecedor);
        compra.setArtigo(artigo);
        compra.setUtilizador(user);
        compra.setTaxaIva(taxaIva);
        compra.setQuantidade(dto.getQuantidade());
        compra.setPrecoUnitario(dto.getPrecoUnitario());
        compra.setTotal(totalComIva);
        compra.setNumeroFaturaFornecedor(dto.getNumeroFaturaFornecedor());
        compra.setDesignacao(dto.getDesignacaoPersonalizada() != null && !dto.getDesignacaoPersonalizada().isBlank()
                ? dto.getDesignacaoPersonalizada() : artigo.getNome());

        // 🛡️ AQUI: Definimos como Pendente e sem conta bancária associada ainda
        compra.setEstadoPagamento(EstadoPagamento.PENDENTE);
        compra.setContaBancaria(null);

        // Analítica
        if (dto.getCentroCustoId() != null) {
            centroCustoRepository.findByIdAndUtilizadorId(dto.getCentroCustoId(), utilizadorId).ifPresent(compra::setCentroCusto);
        }
        if (dto.getSeccaoHomoId() != null) {
            seccaoHomoRepository.findByIdAndUtilizadorId(dto.getSeccaoHomoId(), utilizadorId).ifPresent(compra::setSeccaoHomo);
        }

        Compra compraGuardada = compraRepository.save(compra);

        return converterParaDTO(compraGuardada);
    }

    @Transactional(readOnly = true)
    public CompraResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        Compra compra = compraRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Compra não encontrada ou acesso negado."));

        return converterParaDTO(compra);
    }

    @Transactional
    public void eliminar(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        Compra compra = compraRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Compra não encontrada ou acesso negado."));

        // Se a compra já estiver PAGA, convém não deixar eliminar ou reverter o financeiro primeiro
        if (compra.getEstadoPagamento() == EstadoPagamento.PAGO) {
            throw new IllegalStateException("Não é possível eliminar uma compra que já foi paga na tesouraria.");
        }

        compraRepository.delete(compra);
    }

    @Transactional(readOnly = true)
    public Page<CompraResponseDTO> listarMinhasCompras(int pagina, int tamanho) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("dataCompra").descending());

        return compraRepository.findAllByUtilizadorId(utilizadorId, pageable).map(this::converterParaDTO);
    }

    public List<TxIva> listarTaxasIva() {
        return txIvaRepository.findAll();
    }

    private CompraResponseDTO converterParaDTO(Compra c) {
        CompraResponseDTO dto = new CompraResponseDTO();
        dto.setId(c.getId());
        dto.setDataCompra(c.getDataCompra());
        dto.setNumeroFaturaFornecedor(c.getNumeroFaturaFornecedor());
        dto.setDesignacao(c.getDesignacao());
        dto.setQuantidade(c.getQuantidade());
        dto.setPrecoUnitario(c.getPrecoUnitario());
        dto.setTotal(c.getTotal());
        dto.setEstadoPagamento(c.getEstadoPagamento().name());

        if (c.getFornecedor() != null) {
            dto.setFornecedorId(c.getFornecedor().getId());
            dto.setFornecedorNome(c.getFornecedor().getNome());
        }

        if (c.getArtigo() != null) {
            dto.setArtigoId(c.getArtigo().getId());
            dto.setArtigoNome(c.getArtigo().getNome());
        }

        if (c.getTaxaIva() != null) {
            dto.setTaxaIvaId(c.getTaxaIva().getId());
            dto.setTaxaIvaValor(c.getTaxaIva().getValor());
        }

        if (c.getContaBancaria() != null) {
            dto.setContaBancariaId(c.getContaBancaria().getId());
            dto.setContaBancariaNome(c.getContaBancaria().getNome());
        }

        if (c.getCentroCusto() != null) {
            dto.setCentroCustoId(c.getCentroCusto().getId());
            dto.setCentroCustoCodigo(c.getCentroCusto().getCodigo());
        }

        if (c.getSeccaoHomo() != null) {
            dto.setSeccaoHomoId(c.getSeccaoHomo().getId());
            dto.setSeccaoHomoCodigo(c.getSeccaoHomo().getCodigo());
        }

        return dto;
    }
}