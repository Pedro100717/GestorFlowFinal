package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.VendaDTO;
import pt.gestorflow.backend.dto.VendaResponseDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendaService {

    private final VendaRepository vendaRepository;
    private final ClienteRepository clienteRepository;
    private final ArtigoRepository artigoRepository;
    private final TxIvaRepository txIvaRepository;
    private final MovimentoStockRepository movimentoStockRepository;
    private final CentroCustoRepository centroCustoRepository;
    private final SeccaoHomoRepository seccaoHomoRepository;
    private final LinhaVendaRepository linhaVendaRepository;
    private final MovimentoPlaneadoRepository movimentoPlaneadoRepository; // 🚀 Injetado para abater os planos

    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;
    private final ArtigoService artigoService;

    @Transactional
    public VendaResponseDTO registarVenda(VendaDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        Cliente cliente = clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado ou acesso negado."));

        Venda venda = new Venda();
        venda.setCliente(cliente);
        venda.setUtilizador(user);

        venda.setContaBancaria(null);
        venda.setEstadoPagamento(EstadoPagamento.PENDENTE);
        venda.setDataVenda(dto.getDataVenda() != null ? dto.getDataVenda().atStartOfDay() : LocalDateTime.now());

        // 🚀 MAPEAMENTO DO VENCIMENTO
        venda.setDataVencimento(dto.getDataVencimento() != null ? dto.getDataVencimento().atStartOfDay() : venda.getDataVenda());

        // 🚀 GRAVAR O ELO SECRETO DE RASTREABILIDADE
        venda.setPlanoOrigemId(dto.getPlanoOrigemId());

        if (dto.getCentroCustoId() != null) {
            CentroCusto centro = centroCustoRepository.findByIdAndUtilizadorId(dto.getCentroCustoId(), utilizadorId)
                    .orElseThrow(() -> new EntityNotFoundException("Centro de Custo não encontrado ou acesso negado."));
            venda.setCentroCusto(centro);
        }

        if (dto.getSeccaoHomoId() != null) {
            SeccaoHomo seccao = seccaoHomoRepository.findByIdAndUtilizadorId(dto.getSeccaoHomoId(), utilizadorId)
                    .orElseThrow(() -> new EntityNotFoundException("Secção Homogénea não encontrada ou acesso negado."));
            venda.setSeccaoHomo(seccao);
        }

        BigDecimal totalGeralSemIva = BigDecimal.ZERO;
        BigDecimal totalGeralComIva = BigDecimal.ZERO;

        if (venda.getLinhas() == null) venda.setLinhas(new ArrayList<>());

        for (VendaDTO.LinhaVendaDTO linhaDto : dto.getLinhas()) {
            Artigo artigo = artigoRepository.findByIdAndUtilizadorId(linhaDto.getArtigoId(), utilizadorId)
                    .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado: " + linhaDto.getArtigoId()));

            TxIva taxaIva = txIvaRepository.findById(linhaDto.getTaxaIvaId())
                    .orElseThrow(() -> new EntityNotFoundException("Taxa de IVA não encontrada"));

            if (artigo instanceof Mercadoria mercadoria) {
                artigoService.removerStock(mercadoria.getId(), linhaDto.getQuantidade());

                MovimentoStock mov = new MovimentoStock();
                mov.setMercadoria(mercadoria);
                mov.setUtilizador(user);
                mov.setTipo(MovimentoStock.TipoMovimentoStock.SAIDA);
                mov.setQuantidade(linhaDto.getQuantidade());
                mov.setStockAposMovimento(mercadoria.getStockAtual());
                mov.setMotivo("Venda a Cliente: " + cliente.getNome());
                movimentoStockRepository.save(mov);
            }

            BigDecimal totalLinhaSemIva = linhaDto.getPrecoUnitario().multiply(linhaDto.getQuantidade());
            BigDecimal fatorIva = taxaIva.getValor().divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP).add(BigDecimal.ONE);
            BigDecimal totalLinhaComIva = totalLinhaSemIva.multiply(fatorIva);

            LinhaVenda linha = new LinhaVenda();
            linha.setVenda(venda);
            linha.setArtigo(artigo);
            linha.setTaxaIva(taxaIva);
            linha.setQuantidade(linhaDto.getQuantidade());
            linha.setPrecoUnitario(linhaDto.getPrecoUnitario());
            linha.setTotalLinhaSemIva(totalLinhaSemIva);
            linha.setTotalLinhaComIva(totalLinhaComIva);
            linha.setDesignacaoPersonalizada(linhaDto.getDesignacaoPersonalizada());

            venda.getLinhas().add(linha);

            totalGeralSemIva = totalGeralSemIva.add(totalLinhaSemIva);
            totalGeralComIva = totalGeralComIva.add(totalLinhaComIva);
        }

        venda.setTotalSemIva(totalGeralSemIva);
        venda.setTotalComIva(totalGeralComIva);

        Venda vendaGuardada = vendaRepository.save(venda);

        // =========================================================================================
        // 🚀 O MOTOR DE ABATE DA TESOURARIA: Faz a linha fantasma desaparecer do mês corrente
        // =========================================================================================
        if (dto.getPlanoOrigemId() != null) {
            movimentoPlaneadoRepository.findByIdAndUtilizadorId(dto.getPlanoOrigemId(), utilizadorId)
                    .ifPresent(plano -> {
                        java.time.LocalDate dataReferencia = plano.getDataUltimoProcessamento() != null
                                ? plano.getDataUltimoProcessamento()
                                : plano.getDataInicio();

                        java.time.LocalDate novaData = switch (plano.getFrequencia()) {
                            case SEMANAL -> dataReferencia.plusWeeks(1);
                            case MENSAL -> dataReferencia.plusMonths(1);
                            case TRIMESTRAL -> dataReferencia.plusMonths(3);
                            case SEMESTRAL -> dataReferencia.plusMonths(6);
                            case ANUAL -> dataReferencia.plusYears(1);
                            case PONTUAL -> dataReferencia.plusYears(100);
                        };

                        plano.setDataUltimoProcessamento(novaData);
                        movimentoPlaneadoRepository.save(plano);
                    });
        }
        // =========================================================================================

        return converterParaDTO(vendaGuardada);
    }

    @Transactional
    public VendaResponseDTO atualizarVenda(Long id, VendaDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        Venda venda = vendaRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Venda não encontrada ou acesso negado."));

        if (venda.getEstadoPagamento() != EstadoPagamento.PENDENTE) {
            throw new IllegalStateException("Não podes alterar uma venda que já tenha pagamentos na Tesouraria.");
        }

        Cliente cliente = clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado."));
        venda.setCliente(cliente);
        venda.setDataVenda(dto.getDataVenda() != null ? dto.getDataVenda().atStartOfDay() : venda.getDataVenda());

        // 🚀 MAPEAMENTO DO VENCIMENTO NA EDIÇÃO
        venda.setDataVencimento(dto.getDataVencimento() != null ? dto.getDataVencimento().atStartOfDay() : venda.getDataVencimento());

        for (LinhaVenda linhaAntiga : venda.getLinhas()) {
            if (linhaAntiga.getArtigo() instanceof Mercadoria mercadoria) {
                artigoService.adicionarStock(mercadoria.getId(), linhaAntiga.getQuantidade());

                MovimentoStock mov = new MovimentoStock();
                mov.setMercadoria(mercadoria);
                mov.setUtilizador(user);
                mov.setTipo(MovimentoStock.TipoMovimentoStock.ENTRADA);
                mov.setQuantidade(linhaAntiga.getQuantidade());
                mov.setStockAposMovimento(mercadoria.getStockAtual());
                mov.setMotivo("Edição Venda #" + venda.getId() + " (Devolução Antiga)");
                movimentoStockRepository.save(mov);
            }
        }

        linhaVendaRepository.deleteAll(venda.getLinhas());
        venda.getLinhas().clear();

        BigDecimal totalGeralSemIva = BigDecimal.ZERO;
        BigDecimal totalGeralComIva = BigDecimal.ZERO;

        for (VendaDTO.LinhaVendaDTO linhaDto : dto.getLinhas()) {
            Artigo artigo = artigoRepository.findByIdAndUtilizadorId(linhaDto.getArtigoId(), utilizadorId)
                    .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado."));
            TxIva taxaIva = txIvaRepository.findById(linhaDto.getTaxaIvaId())
                    .orElseThrow(() -> new EntityNotFoundException("Taxa de IVA não encontrada"));

            if (artigo instanceof Mercadoria mercadoria) {
                artigoService.removerStock(mercadoria.getId(), linhaDto.getQuantidade());

                MovimentoStock mov = new MovimentoStock();
                mov.setMercadoria(mercadoria);
                mov.setUtilizador(user);
                mov.setTipo(MovimentoStock.TipoMovimentoStock.SAIDA);
                mov.setQuantidade(linhaDto.getQuantidade());
                mov.setStockAposMovimento(mercadoria.getStockAtual());
                mov.setMotivo("Edição Venda #" + venda.getId() + " (Nova Saída)");
                movimentoStockRepository.save(mov);
            }

            BigDecimal totalLinhaSemIva = linhaDto.getPrecoUnitario().multiply(linhaDto.getQuantidade());
            BigDecimal fatorIva = taxaIva.getValor().divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP).add(BigDecimal.ONE);
            BigDecimal totalLinhaComIva = totalLinhaSemIva.multiply(fatorIva);

            LinhaVenda linha = new LinhaVenda();
            linha.setVenda(venda);
            linha.setArtigo(artigo);
            linha.setTaxaIva(taxaIva);
            linha.setQuantidade(linhaDto.getQuantidade());
            linha.setPrecoUnitario(linhaDto.getPrecoUnitario());
            linha.setTotalLinhaSemIva(totalLinhaSemIva);
            linha.setTotalLinhaComIva(totalLinhaComIva);
            linha.setDesignacaoPersonalizada(linhaDto.getDesignacaoPersonalizada());

            venda.getLinhas().add(linha);

            totalGeralSemIva = totalGeralSemIva.add(totalLinhaSemIva);
            totalGeralComIva = totalGeralComIva.add(totalLinhaComIva);
        }

        venda.setTotalSemIva(totalGeralSemIva);
        venda.setTotalComIva(totalGeralComIva);

        if (dto.getCentroCustoId() != null) {
            centroCustoRepository.findByIdAndUtilizadorId(dto.getCentroCustoId(), utilizadorId).ifPresent(venda::setCentroCusto);
        } else {
            venda.setCentroCusto(null);
        }

        if (dto.getSeccaoHomoId() != null) {
            seccaoHomoRepository.findByIdAndUtilizadorId(dto.getSeccaoHomoId(), utilizadorId).ifPresent(venda::setSeccaoHomo);
        } else {
            venda.setSeccaoHomo(null);
        }

        return converterParaDTO(vendaRepository.save(venda));
    }

    @Transactional
    public void anularVenda(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        Venda venda = vendaRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Venda não encontrada ou acesso negado."));

        if (venda.getEstadoPagamento() == EstadoPagamento.PAGO || venda.getEstadoPagamento() == EstadoPagamento.PARCIALMENTE_PAGO) {
            throw new IllegalStateException("Não é possível anular uma venda que já foi recebida na tesouraria (parcial ou totalmente).");
        }

        for (LinhaVenda linha : venda.getLinhas()) {
            if (linha.getArtigo() instanceof Mercadoria mercadoria) {
                artigoService.adicionarStock(mercadoria.getId(), linha.getQuantidade());

                MovimentoStock mov = new MovimentoStock();
                mov.setMercadoria(mercadoria);
                mov.setUtilizador(user);
                mov.setTipo(MovimentoStock.TipoMovimentoStock.ENTRADA);
                mov.setQuantidade(linha.getQuantidade());
                mov.setStockAposMovimento(mercadoria.getStockAtual());
                mov.setMotivo("Reposição por Anulação de Venda #" + venda.getId());
                movimentoStockRepository.save(mov);
            }
        }

        vendaRepository.delete(venda);
    }

    @Transactional(readOnly = true)
    public VendaResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Venda venda = vendaRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Venda não encontrada ou acesso negado."));
        return converterParaDTO(venda);
    }

    public List<TxIva> listarTaxasIva() {
        return txIvaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<VendaResponseDTO> listarMinhasVendas(int pagina, int tamanho) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Pageable pageable = PageRequest.of(pagina, tamanho,
                Sort.by("dataVenda").descending().and(Sort.by("id").descending()));
        return vendaRepository.findAllByUtilizadorId(utilizadorId, pageable).map(this::converterParaDTO);
    }

    private VendaResponseDTO converterParaDTO(Venda v) {
        VendaResponseDTO dto = new VendaResponseDTO();
        dto.setId(v.getId());
        dto.setDataVenda(v.getDataVenda());

        // 🚀 RESPOSTA COM VENCIMENTO E RASTREABILIDADE
        dto.setDataVencimento(v.getDataVencimento());
        dto.setPlanoOrigemId(v.getPlanoOrigemId());

        dto.setTotalSemIva(v.getTotalSemIva());
        dto.setTotalComIva(v.getTotalComIva());
        dto.setEstadoPagamento(v.getEstadoPagamento().name());

        if (v.getCliente() != null) {
            dto.setClienteId(v.getCliente().getId());
            dto.setClienteNome(v.getCliente().getNome());
        }

        if (v.getContaBancaria() != null) {
            dto.setContaBancariaNome(v.getContaBancaria().getNome());
        }

        if (v.getCentroCusto() != null) {
            dto.setCentroCustoId(v.getCentroCusto().getId());
            dto.setCentroCustoCodigo(v.getCentroCusto().getCodigo());
        }

        if (v.getSeccaoHomo() != null) {
            dto.setSeccaoHomoId(v.getSeccaoHomo().getId());
            dto.setSeccaoHomoCodigo(v.getSeccaoHomo().getCodigo());
        }

        if (v.getLinhas() != null) {
            dto.setLinhas(v.getLinhas().stream().map(linha -> {
                VendaResponseDTO.LinhaVendaResponseDTO lDto = new VendaResponseDTO.LinhaVendaResponseDTO();
                lDto.setId(linha.getId());
                lDto.setQuantidade(linha.getQuantidade());
                lDto.setPrecoUnitario(linha.getPrecoUnitario());
                lDto.setTotalLinhaSemIva(linha.getTotalLinhaSemIva());
                lDto.setTotalLinhaComIva(linha.getTotalLinhaComIva());
                lDto.setDesignacaoPersonalizada(linha.getDesignacaoPersonalizada());

                if (linha.getArtigo() != null) {
                    lDto.setArtigoId(linha.getArtigo().getId());
                    lDto.setArtigoNome(linha.getArtigo().getNome());
                }
                if (linha.getTaxaIva() != null) {
                    lDto.setTaxaIvaValor(linha.getTaxaIva().getValor());
                }
                return lDto;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}