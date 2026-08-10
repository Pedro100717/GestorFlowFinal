package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.CompraDTO;
import pt.gestorflow.backend.dto.CompraResponseDTO;
import pt.gestorflow.backend.dto.LinhaCompraDTO;
import pt.gestorflow.backend.dto.LinhaCompraResponseDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j // 🚀 Anotação Mágica do Lombok
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
        log.info("Início do registo de nova Compra para o fornecedor ID: {} (Utilizador: {})", dto.getFornecedorId(), utilizadorId);

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        Fornecedor fornecedor = fornecedorRepository.findByIdAndUtilizadorId(dto.getFornecedorId(), utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado ou acesso negado."));

        Compra compra = new Compra();
        compra.setDataCompra(dto.getDataCompra() != null ? dto.getDataCompra() : LocalDate.now());
        compra.setDataVencimento(dto.getDataVencimento() != null ? dto.getDataVencimento() : compra.getDataCompra());
        compra.setFornecedor(fornecedor);
        compra.setUtilizador(user);
        compra.setNumeroFaturaFornecedor(dto.getNumeroFaturaFornecedor());
        compra.setEstadoPagamento(EstadoPagamento.PENDENTE);
        compra.setPlanoOrigemId(dto.getPlanoOrigemId());

        BigDecimal totalFatura = BigDecimal.ZERO;

        log.debug("A processar {} linhas de compra...", dto.getLinhas().size());

        for (LinhaCompraDTO linhaDto : dto.getLinhas()) {
            Artigo artigo = artigoRepository.findByIdAndUtilizadorId(linhaDto.getArtigoId(), utilizadorId)
                    .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado: " + linhaDto.getArtigoId()));

            TxIva taxaIva = txIvaRepository.findById(linhaDto.getTaxaIvaId())
                    .orElseThrow(() -> new EntityNotFoundException("Taxa de IVA não encontrada: " + linhaDto.getTaxaIvaId()));

            BigDecimal totalSemIva = linhaDto.getQuantidade().multiply(linhaDto.getPrecoUnitario());
            BigDecimal fatorIva = taxaIva.getValor().divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE);
            BigDecimal totalLinhaComIva = totalSemIva.multiply(fatorIva).setScale(2, RoundingMode.HALF_UP);

            totalFatura = totalFatura.add(totalLinhaComIva);

            processarEntradaStock(artigo, linhaDto.getQuantidade(), linhaDto.getPrecoUnitario(), user, fornecedor.getNome());

            LinhaCompra linha = new LinhaCompra();
            linha.setArtigo(artigo);
            linha.setTaxaIva(taxaIva);
            linha.setQuantidade(linhaDto.getQuantidade());
            linha.setPrecoUnitario(linhaDto.getPrecoUnitario());
            linha.setTotalLinha(totalLinhaComIva);
            linha.setDesignacaoPersonalizada(linhaDto.getDesignacaoPersonalizada());

            if (linhaDto.getCentroCustoId() != null) {
                centroCustoRepository.findByIdAndUtilizadorId(linhaDto.getCentroCustoId(), utilizadorId).ifPresent(linha::setCentroCusto);
            }
            if (linhaDto.getSeccaoHomoId() != null) {
                seccaoHomoRepository.findByIdAndUtilizadorId(linhaDto.getSeccaoHomoId(), utilizadorId).ifPresent(linha::setSeccaoHomo);
            }

            compra.addLinha(linha);
        }

        compra.setTotal(totalFatura);
        Compra compraGuardada = compraRepository.save(compra);

        if (dto.getPlanoOrigemId() != null) {
            movimentoPlaneadoRepository.findByIdAndUtilizadorId(dto.getPlanoOrigemId(), utilizadorId)
                    .ifPresent(plano -> {
                        LocalDate dataAIgnorar = dto.getDataCompra() != null ? dto.getDataCompra() : LocalDate.now();
                        plano.getDatasIgnoradas().add(dataAIgnorar);
                        movimentoPlaneadoRepository.save(plano);
                        log.debug("Abate realizado no Plano Origem ID: {}", plano.getId());
                    });
        }

        log.debug("Compra ID: {} registada com sucesso. Valor total: {}", compraGuardada.getId(), totalFatura);
        return converterParaDTO(compraGuardada);
    }

    @Transactional
    public CompraResponseDTO atualizarCompra(Long id, CompraDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        log.info("Pedido de atualização da Compra ID: {} (Utilizador: {})", id, utilizadorId);

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        Compra compra = compraRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Compra não encontrada ou acesso negado."));

        if (compra.getEstadoPagamento() != EstadoPagamento.PENDENTE) {
            // 🚀 Corrigido para IllegalArgumentException e adicionado log.warn
            log.warn("Bloqueada edição da Compra ID: {} (Estado atual: {})", id, compra.getEstadoPagamento());
            throw new IllegalArgumentException("Apenas faturas de compra PENDENTES podem ser editadas.");
        }

        Fornecedor fornecedor = fornecedorRepository.findByIdAndUtilizadorId(dto.getFornecedorId(), utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado."));

        // Reverter stock
        for (LinhaCompra linhaAntiga : compra.getLinhas()) {
            if (linhaAntiga.getArtigo() instanceof Mercadoria mercAntiga) {
                artigoService.removerStock(mercAntiga.getId(), linhaAntiga.getQuantidade());
                registarMovimentoStock(mercAntiga, user, MovimentoStock.TipoMovimentoStock.SAIDA,
                        linhaAntiga.getQuantidade(), "Edição Compra #" + compra.getId() + " (Estorno Antigo)");
            }
        }

        compra.getLinhas().clear();
        BigDecimal totalFatura = BigDecimal.ZERO;

        for (LinhaCompraDTO linhaDto : dto.getLinhas()) {
            Artigo artigo = artigoRepository.findByIdAndUtilizadorId(linhaDto.getArtigoId(), utilizadorId)
                    .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado."));
            TxIva taxaIva = txIvaRepository.findById(linhaDto.getTaxaIvaId())
                    .orElseThrow(() -> new EntityNotFoundException("Taxa de IVA não encontrada."));

            BigDecimal totalSemIva = linhaDto.getQuantidade().multiply(linhaDto.getPrecoUnitario());
            BigDecimal fatorIva = taxaIva.getValor().divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE);
            BigDecimal totalLinhaComIva = totalSemIva.multiply(fatorIva).setScale(2, RoundingMode.HALF_UP);
            totalFatura = totalFatura.add(totalLinhaComIva);

            processarEntradaStock(artigo, linhaDto.getQuantidade(), linhaDto.getPrecoUnitario(), user, "Edição Compra #" + compra.getId());

            LinhaCompra novaLinha = new LinhaCompra();
            novaLinha.setArtigo(artigo);
            novaLinha.setTaxaIva(taxaIva);
            novaLinha.setQuantidade(linhaDto.getQuantidade());
            novaLinha.setPrecoUnitario(linhaDto.getPrecoUnitario());
            novaLinha.setTotalLinha(totalLinhaComIva);
            novaLinha.setDesignacaoPersonalizada(linhaDto.getDesignacaoPersonalizada());

            if (linhaDto.getCentroCustoId() != null) {
                centroCustoRepository.findByIdAndUtilizadorId(linhaDto.getCentroCustoId(), utilizadorId).ifPresent(novaLinha::setCentroCusto);
            }
            if (linhaDto.getSeccaoHomoId() != null) {
                seccaoHomoRepository.findByIdAndUtilizadorId(linhaDto.getSeccaoHomoId(), utilizadorId).ifPresent(novaLinha::setSeccaoHomo);
            }

            compra.addLinha(novaLinha);
        }

        compra.setDataCompra(dto.getDataCompra() != null ? dto.getDataCompra() : compra.getDataCompra());
        compra.setDataVencimento(dto.getDataVencimento() != null ? dto.getDataVencimento() : compra.getDataVencimento());
        compra.setFornecedor(fornecedor);
        compra.setNumeroFaturaFornecedor(dto.getNumeroFaturaFornecedor());
        compra.setTotal(totalFatura);

        Compra atualizada = compraRepository.save(compra);
        log.debug("Compra ID: {} atualizada com sucesso. Novo valor: {}", atualizada.getId(), totalFatura);

        return converterParaDTO(atualizada);
    }

    @Transactional
    public void eliminar(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        log.info("Aviso Crítico: Pedido de eliminação da Compra ID: {} (Utilizador: {})", id, utilizadorId);

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        Compra compra = compraRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Compra não encontrada ou acesso negado."));

        if (compra.getEstadoPagamento() == EstadoPagamento.PAGO || compra.getEstadoPagamento() == EstadoPagamento.PARCIALMENTE_PAGO) {
            // 🚀 Corrigido para IllegalArgumentException e adicionado log.warn
            log.warn("Bloqueada eliminação da Compra ID: {}. Fatura já contém pagamentos.", id);
            throw new IllegalArgumentException("Não podes apagar uma compra que já tem pagamentos registados.");
        }

        for (LinhaCompra linha : compra.getLinhas()) {
            if (linha.getArtigo() instanceof Mercadoria mercadoria) {
                artigoService.removerStock(mercadoria.getId(), linha.getQuantidade());
                registarMovimentoStock(mercadoria, user, MovimentoStock.TipoMovimentoStock.SAIDA,
                        linha.getQuantidade(), "Anulação da Compra #" + compra.getId());
            }
        }

        compraRepository.delete(compra);
        log.debug("Compra ID: {} eliminada com sucesso da base de dados.", id);
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
        log.debug("Listagem de compras solicitada pelo utilizador ID: {}. Página: {}", utilizadorId, pagina);

        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("dataCompra").descending());
        return compraRepository.findAllByUtilizadorId(utilizadorId, pageable).map(this::converterParaDTO);
    }

    public List<TxIva> listarTaxasIva() {
        return txIvaRepository.findAll();
    }

    // --- MÉTODOS AUXILIARES ---

    private void processarEntradaStock(Artigo artigo, BigDecimal qtdComprada, BigDecimal precoCompra, Utilizador user, String motivo) {
        if (artigo instanceof Mercadoria mercadoria) {
            BigDecimal stockAtual = mercadoria.getStockAtual() != null ? mercadoria.getStockAtual() : BigDecimal.ZERO;
            BigDecimal custoAtual = artigo.getUltimoPrecoCusto() != null ? artigo.getUltimoPrecoCusto() : BigDecimal.ZERO;

            BigDecimal valorStockExistente = stockAtual.multiply(custoAtual);
            BigDecimal valorNovaCompra = qtdComprada.multiply(precoCompra);
            BigDecimal novoStockTotal = stockAtual.add(qtdComprada);

            if (novoStockTotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal precoMedioPonderado = valorStockExistente.add(valorNovaCompra)
                        .divide(novoStockTotal, 4, RoundingMode.HALF_UP);
                artigo.setUltimoPrecoCusto(precoMedioPonderado);
            } else {
                artigo.setUltimoPrecoCusto(precoCompra);
            }

            mercadoria.setStockAtual(novoStockTotal);
            artigoRepository.save(artigo);
            registarMovimentoStock(mercadoria, user, MovimentoStock.TipoMovimentoStock.ENTRADA, qtdComprada, motivo);
        } else {
            artigo.setUltimoPrecoCusto(precoCompra);
            artigoRepository.save(artigo);
        }
    }

    private void registarMovimentoStock(Mercadoria mercadoria, Utilizador user, MovimentoStock.TipoMovimentoStock tipo, BigDecimal quantidade, String motivo) {
        MovimentoStock mov = new MovimentoStock();
        mov.setMercadoria(mercadoria);
        mov.setUtilizador(user);
        mov.setTipo(tipo);
        mov.setQuantidade(quantidade);
        mov.setStockAposMovimento(mercadoria.getStockAtual());
        mov.setMotivo(motivo);
        movimentoStockRepository.save(mov);
    }

    private CompraResponseDTO converterParaDTO(Compra c) {
        CompraResponseDTO dto = new CompraResponseDTO();
        dto.setId(c.getId());
        dto.setDataCompra(c.getDataCompra());
        dto.setDataVencimento(c.getDataVencimento());
        dto.setDataPrevistaPagamento(c.getDataPrevistaPagamento());
        dto.setNumeroFaturaFornecedor(c.getNumeroFaturaFornecedor());
        dto.setTotal(c.getTotal());
        dto.setEstadoPagamento(c.getEstadoPagamento().name());
        dto.setPlanoOrigemId(c.getPlanoOrigemId());

        if (c.getFornecedor() != null) {
            dto.setFornecedorId(c.getFornecedor().getId());
            dto.setFornecedorNome(c.getFornecedor().getNome());
        }

        if (c.getContaBancaria() != null) {
            dto.setContaBancariaId(c.getContaBancaria().getId());
            dto.setContaBancariaNome(c.getContaBancaria().getNome());
        }

        if (c.getLinhas() != null) {
            List<LinhaCompraResponseDTO> linhasDto = c.getLinhas().stream().map(linha -> {
                LinhaCompraResponseDTO lDto = new LinhaCompraResponseDTO();
                lDto.setId(linha.getId());
                lDto.setQuantidade(linha.getQuantidade());
                lDto.setPrecoUnitario(linha.getPrecoUnitario());
                lDto.setTotalLinha(linha.getTotalLinha());
                lDto.setDesignacaoPersonalizada(linha.getDesignacaoPersonalizada());

                if (linha.getArtigo() != null) {
                    lDto.setArtigoId(linha.getArtigo().getId());
                    lDto.setArtigoNome(linha.getArtigo().getNome());
                }
                if (linha.getTaxaIva() != null) {
                    lDto.setTaxaIvaId(linha.getTaxaIva().getId());
                    lDto.setTaxaIvaValor(linha.getTaxaIva().getValor());
                }
                if (linha.getCentroCusto() != null) {
                    lDto.setCentroCustoId(linha.getCentroCusto().getId());
                    lDto.setCentroCustoCodigo(linha.getCentroCusto().getCodigo());
                    lDto.setCentroCustoNome(linha.getCentroCusto().getNome());
                }
                if (linha.getSeccaoHomo() != null) {
                    lDto.setSeccaoHomoId(linha.getSeccaoHomo().getId());
                    lDto.setSeccaoHomoCodigo(linha.getSeccaoHomo().getCodigo());
                    lDto.setSeccaoHomoNome(linha.getSeccaoHomo().getNome());
                }
                return lDto;
            }).collect(Collectors.toList());

            dto.setLinhas(linhasDto);
        } else {
            dto.setLinhas(new ArrayList<>());
        }

        return dto;
    }
}