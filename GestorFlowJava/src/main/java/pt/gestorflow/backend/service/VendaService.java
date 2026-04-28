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

    // 🚀 Injeções de Segurança
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    @Transactional
    public VendaResponseDTO registarVenda(VendaDTO dto) {
        // 🚀 1. Identidade Blindada
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        // 🛡️ Validação Comercial Segura
        Cliente cliente = clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado ou acesso negado."));

        Venda venda = new Venda();
        venda.setCliente(cliente);
        venda.setUtilizador(user);

        venda.setContaBancaria(null);
        venda.setEstadoPagamento(EstadoPagamento.PENDENTE);
        venda.setDataVenda(dto.getDataVenda() != null ? dto.getDataVenda().atStartOfDay() : LocalDateTime.now());

        // 🛡️ CORREÇÃO CRÍTICA IDOR: Validação do dono do Centro de Custo e Secção Homogénea
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

        // PROCESSAR LINHAS E STOCK
        for (VendaDTO.LinhaVendaDTO linhaDto : dto.getLinhas()) {
            Artigo artigo = artigoRepository.findByIdAndUtilizadorId(linhaDto.getArtigoId(), utilizadorId)
                    .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado: " + linhaDto.getArtigoId()));

            TxIva taxaIva = txIvaRepository.findById(linhaDto.getTaxaIvaId())
                    .orElseThrow(() -> new EntityNotFoundException("Taxa de IVA não encontrada"));

            // Desconta Stock (Logística)
            if (artigo instanceof Mercadoria mercadoria) {
                mercadoria.setStockAtual(mercadoria.getStockAtual().subtract(linhaDto.getQuantidade()));
                artigoRepository.save(mercadoria);

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
            linha.setDesignacaoPersonalizada(linhaDto.getDesignacaoPersonalizada()); // Preservar designação customizada

            venda.getLinhas().add(linha);

            totalGeralSemIva = totalGeralSemIva.add(totalLinhaSemIva);
            totalGeralComIva = totalGeralComIva.add(totalLinhaComIva);
        }

        venda.setTotalSemIva(totalGeralSemIva);
        venda.setTotalComIva(totalGeralComIva);

        Venda vendaGuardada = vendaRepository.save(venda);

        return converterParaDTO(vendaGuardada);
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
        // Ordenação estável
        Pageable pageable = PageRequest.of(pagina, tamanho,
                Sort.by("dataVenda").descending().and(Sort.by("id").descending()));
        return vendaRepository.findAllByUtilizadorId(utilizadorId, pageable).map(this::converterParaDTO);
    }

    @Transactional
    public void anularVenda(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        Venda venda = vendaRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Venda não encontrada ou acesso negado."));

        if (venda.getEstadoPagamento() == EstadoPagamento.PAGO) {
            throw new IllegalStateException("Não é possível anular uma venda que já foi recebida na tesouraria.");
        }

        // 🛡️ CORREÇÃO CRÍTICA LOGÍSTICA: Repor o stock antes de apagar a venda!
        for (LinhaVenda linha : venda.getLinhas()) {
            if (linha.getArtigo() instanceof Mercadoria mercadoria) {
                // Devolve a quantidade ao armazém
                mercadoria.setStockAtual(mercadoria.getStockAtual().add(linha.getQuantidade()));
                artigoRepository.save(mercadoria);

                // Regista o movimento de reposição na auditoria
                MovimentoStock mov = new MovimentoStock();
                mov.setMercadoria(mercadoria);
                mov.setUtilizador(user);
                mov.setTipo(MovimentoStock.TipoMovimentoStock.ENTRADA); // Entrada por anulação
                mov.setQuantidade(linha.getQuantidade());
                mov.setStockAposMovimento(mercadoria.getStockAtual());
                mov.setMotivo("Reposição por Anulação de Venda #" + venda.getId());
                movimentoStockRepository.save(mov);
            }
        }

        vendaRepository.delete(venda);
    }

    private VendaResponseDTO converterParaDTO(Venda v) {
        VendaResponseDTO dto = new VendaResponseDTO();
        dto.setId(v.getId());
        dto.setDataVenda(v.getDataVenda());
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