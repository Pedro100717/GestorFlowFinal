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
import pt.gestorflow.backend.dto.LinhaVendaDTO;
import pt.gestorflow.backend.dto.LinhaVendaResponseDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    private final MovimentoPlaneadoRepository movimentoPlaneadoRepository;

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

        venda.setDataVenda(dto.getDataVenda() != null ? dto.getDataVenda() : LocalDate.now());
        venda.setDataVencimento(dto.getDataVencimento() != null ? dto.getDataVencimento() : venda.getDataVenda());

        venda.setPlanoOrigemId(dto.getPlanoOrigemId());

        BigDecimal totalGeralSemIva = BigDecimal.ZERO;
        BigDecimal totalGeralComIva = BigDecimal.ZERO;

        // 🚀 PROCESSAMENTO LINHA A LINHA
        for (LinhaVendaDTO linhaDto : dto.getLinhas()) {
            Artigo artigo = artigoRepository.findByIdAndUtilizadorId(linhaDto.getArtigoId(), utilizadorId)
                    .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado: " + linhaDto.getArtigoId()));

            TxIva taxaIva = txIvaRepository.findById(linhaDto.getTaxaIvaId())
                    .orElseThrow(() -> new EntityNotFoundException("Taxa de IVA não encontrada"));

            // 1. Desconto de Stock (Se aplicável)
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

            // 2. Cálculos Financeiros
            BigDecimal totalLinhaSemIva = linhaDto.getPrecoUnitario().multiply(linhaDto.getQuantidade());
            BigDecimal fatorIva = taxaIva.getValor().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).add(BigDecimal.ONE);
            BigDecimal totalLinhaComIva = totalLinhaSemIva.multiply(fatorIva).setScale(2, RoundingMode.HALF_UP);

            LinhaVenda linha = new LinhaVenda();
            linha.setArtigo(artigo);
            linha.setTaxaIva(taxaIva);
            linha.setQuantidade(linhaDto.getQuantidade());
            linha.setPrecoUnitario(linhaDto.getPrecoUnitario());
            linha.setTotalLinhaSemIva(totalLinhaSemIva);
            linha.setTotalLinhaComIva(totalLinhaComIva);
            linha.setDesignacaoPersonalizada(linhaDto.getDesignacaoPersonalizada());

            // 3. Analítica na Linha
            if (linhaDto.getCentroCustoId() != null) {
                centroCustoRepository.findByIdAndUtilizadorId(linhaDto.getCentroCustoId(), utilizadorId).ifPresent(linha::setCentroCusto);
            }
            if (linhaDto.getSeccaoHomoId() != null) {
                seccaoHomoRepository.findByIdAndUtilizadorId(linhaDto.getSeccaoHomoId(), utilizadorId).ifPresent(linha::setSeccaoHomo);
            }

            venda.addLinha(linha); // Sincroniza a relação bi-direcional

            totalGeralSemIva = totalGeralSemIva.add(totalLinhaSemIva);
            totalGeralComIva = totalGeralComIva.add(totalLinhaComIva);
        }

        venda.setTotalSemIva(totalGeralSemIva);
        venda.setTotalComIva(totalGeralComIva);

        Venda vendaGuardada = vendaRepository.save(venda);

        // O MOTOR DE ABATE (MÁQUINA DO TEMPO)
        if (dto.getPlanoOrigemId() != null) {
            movimentoPlaneadoRepository.findByIdAndUtilizadorId(dto.getPlanoOrigemId(), utilizadorId)
                    .ifPresent(plano -> {
                        LocalDate dataAIgnorar = dto.getDataVenda() != null ? dto.getDataVenda() : LocalDate.now();
                        plano.getDatasIgnoradas().add(dataAIgnorar);
                        movimentoPlaneadoRepository.save(plano);
                    });
        }

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

        venda.setDataVenda(dto.getDataVenda() != null ? dto.getDataVenda() : venda.getDataVenda());
        venda.setDataVencimento(dto.getDataVencimento() != null ? dto.getDataVencimento() : venda.getDataVencimento());

        // 1. Reverter stock das linhas antigas
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

        // 2. Limpar as linhas antigas (O JPA orphanRemoval trata da eliminação na base de dados)
        venda.getLinhas().clear();

        BigDecimal totalGeralSemIva = BigDecimal.ZERO;
        BigDecimal totalGeralComIva = BigDecimal.ZERO;

        // 3. Aplicar novas linhas
        for (LinhaVendaDTO linhaDto : dto.getLinhas()) {
            Artigo art = artigoRepository.findByIdAndUtilizadorId(linhaDto.getArtigoId(), utilizadorId)
                    .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado."));
            TxIva taxaIva = txIvaRepository.findById(linhaDto.getTaxaIvaId())
                    .orElseThrow(() -> new EntityNotFoundException("Taxa de IVA não encontrada"));

            if (art instanceof Mercadoria mercadoria) {
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
            BigDecimal fatorIva = taxaIva.getValor().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).add(BigDecimal.ONE);
            BigDecimal totalLinhaComIva = totalLinhaSemIva.multiply(fatorIva).setScale(2, RoundingMode.HALF_UP);

            LinhaVenda novaLinha = new LinhaVenda();
            novaLinha.setArtigo(art);
            novaLinha.setTaxaIva(taxaIva);
            novaLinha.setQuantidade(linhaDto.getQuantidade());
            novaLinha.setPrecoUnitario(linhaDto.getPrecoUnitario());
            novaLinha.setTotalLinhaSemIva(totalLinhaSemIva);
            novaLinha.setTotalLinhaComIva(totalLinhaComIva);
            novaLinha.setDesignacaoPersonalizada(linhaDto.getDesignacaoPersonalizada());

            if (linhaDto.getCentroCustoId() != null) {
                centroCustoRepository.findByIdAndUtilizadorId(linhaDto.getCentroCustoId(), utilizadorId).ifPresent(novaLinha::setCentroCusto);
            }
            if (linhaDto.getSeccaoHomoId() != null) {
                seccaoHomoRepository.findByIdAndUtilizadorId(linhaDto.getSeccaoHomoId(), utilizadorId).ifPresent(novaLinha::setSeccaoHomo);
            }

            venda.addLinha(novaLinha);

            totalGeralSemIva = totalGeralSemIva.add(totalLinhaSemIva);
            totalGeralComIva = totalGeralComIva.add(totalLinhaComIva);
        }

        venda.setTotalSemIva(totalGeralSemIva);
        venda.setTotalComIva(totalGeralComIva);

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
        dto.setDataVencimento(v.getDataVencimento());
        dto.setDataPrevistaPagamento(v.getDataPrevistaPagamento());
        dto.setPlanoOrigemId(v.getPlanoOrigemId());
        dto.setTotalSemIva(v.getTotalSemIva());
        dto.setTotalComIva(v.getTotalComIva());
        dto.setEstadoPagamento(v.getEstadoPagamento().name());

        if (v.getCliente() != null) {
            dto.setClienteId(v.getCliente().getId());
            dto.setClienteNome(v.getCliente().getNome());
        }

        if (v.getContaBancaria() != null) {
            dto.setContaBancariaId(v.getContaBancaria().getId());
            dto.setContaBancariaNome(v.getContaBancaria().getNome());
        }

        if (v.getLinhas() != null) {
            dto.setLinhas(v.getLinhas().stream().map(linha -> {
                LinhaVendaResponseDTO lDto = new LinhaVendaResponseDTO();
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
                    lDto.setTaxaIvaId(linha.getTaxaIva().getId());
                    lDto.setTaxaIvaValor(linha.getTaxaIva().getValor());
                }
                if (linha.getCentroCusto() != null) {
                    lDto.setCentroCustoId(linha.getCentroCusto().getId());
                    lDto.setCentroCustoCodigo(linha.getCentroCusto().getCodigo());
                }
                if (linha.getSeccaoHomo() != null) {
                    lDto.setSeccaoHomoId(linha.getSeccaoHomo().getId());
                    lDto.setSeccaoHomoCodigo(linha.getSeccaoHomo().getCodigo());
                }
                return lDto;
            }).collect(Collectors.toList()));
        } else {
            dto.setLinhas(new ArrayList<>());
        }

        return dto;
    }
}