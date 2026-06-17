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
import java.time.LocalDate;
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
    private final MovimentoPlaneadoRepository movimentoPlaneadoRepository;

    private final ArtigoService artigoService;
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    @Transactional
    public CompraResponseDTO registarCompra(CompraDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

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

            BigDecimal valorStockExistente = stockAtual.multiply(custoAtual);
            BigDecimal valorNovaCompra = qtdComprada.multiply(precoCompra);
            BigDecimal novoStockTotal = stockAtual.add(qtdComprada);

            if (novoStockTotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal precoMedioPonderado = valorStockExistente.add(valorNovaCompra)
                        .divide(novoStockTotal, 4, java.math.RoundingMode.HALF_UP);
                artigo.setUltimoPrecoCusto(precoMedioPonderado);
            } else {
                artigo.setUltimoPrecoCusto(precoCompra);
            }

            mercadoria.setStockAtual(novoStockTotal);
            artigoRepository.save(artigo);

            MovimentoStock mov = new MovimentoStock();
            mov.setMercadoria(mercadoria);
            mov.setUtilizador(user);
            mov.setTipo(MovimentoStock.TipoMovimentoStock.ENTRADA);
            mov.setQuantidade(qtdComprada);
            mov.setStockAposMovimento(mercadoria.getStockAtual());
            mov.setMotivo("Compra a Fornecedor: " + fornecedor.getNome());
            movimentoStockRepository.save(mov);
        } else {
            artigo.setUltimoPrecoCusto(dto.getPrecoUnitario());
            artigoRepository.save(artigo);
        }

        BigDecimal totalSemIva = dto.getQuantidade().multiply(dto.getPrecoUnitario());
        BigDecimal fatorIva = taxaIva.getValor().divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE);
        BigDecimal totalComIva = totalSemIva.multiply(fatorIva);

        Compra compra = new Compra();

        // 🚀 CORRIGIDO: Atribuição direta de LocalDate puro, sem .atStartOfDay()
        compra.setDataCompra(dto.getDataCompra() != null ? dto.getDataCompra() : LocalDate.now());
        compra.setDataVencimento(dto.getDataVencimento() != null ? dto.getDataVencimento() : compra.getDataCompra());

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

        compra.setEstadoPagamento(EstadoPagamento.PENDENTE);
        compra.setContaBancaria(null);

        compra.setPlanoOrigemId(dto.getPlanoOrigemId());

        if (dto.getCentroCustoId() != null) {
            centroCustoRepository.findByIdAndUtilizadorId(dto.getCentroCustoId(), utilizadorId).ifPresent(compra::setCentroCusto);
        }
        if (dto.getSeccaoHomoId() != null) {
            seccaoHomoRepository.findByIdAndUtilizadorId(dto.getSeccaoHomoId(), utilizadorId).ifPresent(compra::setSeccaoHomo);
        }

        Compra compraGuardada = compraRepository.save(compra);

        // =========================================================================================
        // 🚀 O MOTOR DE ABATE (MÁQUINA DO TEMPO) COM LOCALDATE PURO
        // =========================================================================================
        if (dto.getPlanoOrigemId() != null) {
            movimentoPlaneadoRepository.findByIdAndUtilizadorId(dto.getPlanoOrigemId(), utilizadorId)
                    .ifPresent(plano -> {
                        LocalDate dataAIgnorar = dto.getDataCompra() != null ? dto.getDataCompra() : LocalDate.now();
                        plano.getDatasIgnoradas().add(dataAIgnorar);
                        movimentoPlaneadoRepository.save(plano);
                    });
        }

        return converterParaDTO(compraGuardada);
    }

    @Transactional
    public CompraResponseDTO atualizarCompra(Long id, CompraDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        Compra compra = compraRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Compra não encontrada ou acesso negado."));

        if (compra.getEstadoPagamento() != EstadoPagamento.PENDENTE) {
            throw new IllegalStateException("Apenas faturas de compra PENDENTES podem ser editadas.");
        }

        Fornecedor fornecedor = fornecedorRepository.findByIdAndUtilizadorId(dto.getFornecedorId(), utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado."));
        Artigo novoArtigo = artigoRepository.findByIdAndUtilizadorId(dto.getArtigoId(), utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado."));
        TxIva taxaIva = txIvaRepository.findById(dto.getTaxaIvaId())
                .orElseThrow(() -> new EntityNotFoundException("Taxa de IVA não encontrada."));

        if (compra.getArtigo() instanceof Mercadoria mercAntiga) {
            artigoService.removerStock(mercAntiga.getId(), compra.getQuantidade());
            MovimentoStock movSaida = new MovimentoStock();
            movSaida.setMercadoria(mercAntiga);
            movSaida.setUtilizador(user);
            movSaida.setTipo(MovimentoStock.TipoMovimentoStock.SAIDA);
            movSaida.setQuantidade(compra.getQuantidade());
            movSaida.setStockAposMovimento(mercAntiga.getStockAtual().subtract(compra.getQuantidade()));
            movSaida.setMotivo("Edição Compra #" + compra.getId() + " (Estorno Antigo)");
            movimentoStockRepository.save(movSaida);
        }

        if (novoArtigo instanceof Mercadoria mercNova) {
            artigoService.adicionarStock(mercNova.getId(), dto.getQuantidade());
            MovimentoStock movEntrada = new MovimentoStock();
            movEntrada.setMercadoria(mercNova);
            movEntrada.setUtilizador(user);
            movEntrada.setTipo(MovimentoStock.TipoMovimentoStock.ENTRADA);
            movEntrada.setQuantidade(dto.getQuantidade());
            movEntrada.setStockAposMovimento(mercNova.getStockAtual().add(dto.getQuantidade()));
            movEntrada.setMotivo("Edição Compra #" + compra.getId() + " (Nova Quantidade)");
            movimentoStockRepository.save(movEntrada);
        }

        BigDecimal totalSemIva = dto.getQuantidade().multiply(dto.getPrecoUnitario());
        BigDecimal fatorIva = taxaIva.getValor().divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE);
        BigDecimal totalComIva = totalSemIva.multiply(fatorIva);

        // 🚀 CORRIGIDO: Atribuição direta de LocalDate puro na edição, sem .atStartOfDay()
        compra.setDataCompra(dto.getDataCompra() != null ? dto.getDataCompra() : compra.getDataCompra());
        compra.setDataVencimento(dto.getDataVencimento() != null ? dto.getDataVencimento() : compra.getDataVencimento());

        compra.setFornecedor(fornecedor);
        compra.setArtigo(novoArtigo);
        compra.setTaxaIva(taxaIva);
        compra.setQuantidade(dto.getQuantidade());
        compra.setPrecoUnitario(dto.getPrecoUnitario());
        compra.setTotal(totalComIva);
        compra.setNumeroFaturaFornecedor(dto.getNumeroFaturaFornecedor());
        compra.setDesignacao(dto.getDesignacaoPersonalizada() != null && !dto.getDesignacaoPersonalizada().isBlank()
                ? dto.getDesignacaoPersonalizada() : novoArtigo.getNome());

        if (dto.getCentroCustoId() != null) {
            centroCustoRepository.findByIdAndUtilizadorId(dto.getCentroCustoId(), utilizadorId).ifPresent(compra::setCentroCusto);
        }
        if (dto.getSeccaoHomoId() != null) {
            seccaoHomoRepository.findByIdAndUtilizadorId(dto.getSeccaoHomoId(), utilizadorId).ifPresent(compra::setSeccaoHomo);
        }

        return converterParaDTO(compraRepository.save(compra));
    }

    @Transactional
    public void eliminar(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        Compra compra = compraRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Compra não encontrada ou acesso negado."));

        if (compra.getEstadoPagamento() == EstadoPagamento.PAGO || compra.getEstadoPagamento() == EstadoPagamento.PARCIALMENTE_PAGO) {
            throw new IllegalStateException("Não podes apagar uma compra que já tem pagamentos registados.");
        }

        if (compra.getArtigo() instanceof Mercadoria mercadoria) {
            artigoService.removerStock(compra.getArtigo().getId(), compra.getQuantidade());

            MovimentoStock mov = new MovimentoStock();
            mov.setMercadoria(mercadoria);
            mov.setUtilizador(user);
            mov.setTipo(MovimentoStock.TipoMovimentoStock.SAIDA);
            mov.setQuantidade(compra.getQuantidade());
            mov.setStockAposMovimento(mercadoria.getStockAtual().subtract(compra.getQuantidade()));
            mov.setMotivo("Anulação da Compra #" + compra.getId());
            movimentoStockRepository.save(mov);
        }

        compraRepository.delete(compra);
    }

    @Transactional(readOnly = true)
    public CompraResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Compra compra = compraRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Compra não encontrada ou acesso negado."));
        return converterParaDTO(compra);
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
        dto.setDataVencimento(c.getDataVencimento());
        dto.setNumeroFaturaFornecedor(c.getNumeroFaturaFornecedor());
        dto.setDesignacao(c.getDesignacao());
        dto.setQuantidade(c.getQuantidade());
        dto.setPrecoUnitario(c.getPrecoUnitario());
        dto.setTotal(c.getTotal());
        dto.setEstadoPagamento(c.getEstadoPagamento().name());
        dto.setPlanoOrigemId(c.getPlanoOrigemId());

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