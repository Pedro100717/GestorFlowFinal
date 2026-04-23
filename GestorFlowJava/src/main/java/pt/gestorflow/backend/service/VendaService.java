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
    // Removida a dependência de contaRepository e movimentoRepository para o registo inicial

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Transactional
    public VendaResponseDTO registarVenda(VendaDTO dto) {
        Utilizador user = getUtilizadorLogado();

        // 🛡️ Validação Comercial
        Cliente cliente = clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado ou acesso negado."));

        Venda venda = new Venda();
        venda.setCliente(cliente);
        venda.setUtilizador(user);

        // 🛡️ AQUI: A venda nasce sem conta bancária e com estado PENDENTE
        venda.setContaBancaria(null);
        venda.setEstadoPagamento(EstadoPagamento.PENDENTE);

        venda.setDataVenda(dto.getDataVenda() != null ? dto.getDataVenda().atStartOfDay() : LocalDateTime.now());

        if (dto.getCentroCustoId() != null) {
            CentroCusto centro = centroCustoRepository.findById(dto.getCentroCustoId())
                    .orElseThrow(() -> new EntityNotFoundException("Centro de Custo não encontrado."));
            venda.setCentroCusto(centro);
        }

        if (dto.getSeccaoHomoId() != null) {
            SeccaoHomo seccao = seccaoHomoRepository.findById(dto.getSeccaoHomoId())
                    .orElseThrow(() -> new EntityNotFoundException("Secção Homogénea não encontrada."));
            venda.setSeccaoHomo(seccao);
        }

        BigDecimal totalGeralSemIva = BigDecimal.ZERO;
        BigDecimal totalGeralComIva = BigDecimal.ZERO;

        if(venda.getLinhas() == null) venda.setLinhas(new ArrayList<>());

        // 🛡️ PROCESSAR AS MÚLTIPLAS LINHAS (Logística de Stock e Faturação)
        for (VendaDTO.LinhaVendaDTO linhaDto : dto.getLinhas()) {
            Artigo artigo = artigoRepository.findByIdAndUtilizadorId(linhaDto.getArtigoId(), user.getId())
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

            // Calcula Totais da Linha
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

            venda.getLinhas().add(linha);

            totalGeralSemIva = totalGeralSemIva.add(totalLinhaSemIva);
            totalGeralComIva = totalGeralComIva.add(totalLinhaComIva);
        }

        venda.setTotalSemIva(totalGeralSemIva);
        venda.setTotalComIva(totalGeralComIva);

        Venda vendaGuardada = vendaRepository.save(venda);

        // ❌ Removida a criação de Movimento e alteração de saldo bancário.
        // O dinheiro só entra no sistema quando a Tesouraria confirmar o documento.

        return converterParaDTO(vendaGuardada);
    }

    @Transactional(readOnly = true)
    public VendaResponseDTO buscarPorId(Long id) {
        Venda venda = vendaRepository.findByIdAndUtilizadorId(id, getUtilizadorLogado().getId())
                .orElseThrow(() -> new EntityNotFoundException("Venda não encontrada."));
        return converterParaDTO(venda);
    }

    public List<TxIva> listarTaxasIva() {
        return txIvaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<VendaResponseDTO> listarMinhasVendas(int pagina, int tamanho) {
        Utilizador user = getUtilizadorLogado();
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("dataVenda").descending());
        return vendaRepository.findAllByUtilizadorId(user.getId(), pageable).map(this::converterParaDTO);
    }

    @Transactional
    public void anularVenda(Long id) {
        Venda venda = vendaRepository.findByIdAndUtilizadorId(id, getUtilizadorLogado().getId())
                .orElseThrow(() -> new EntityNotFoundException("Venda não encontrada."));

        // 🛡️ Segurança: Não permite apagar se já houver dinheiro envolvido (PAGO)
        if (venda.getEstadoPagamento() == EstadoPagamento.PAGO) {
            throw new IllegalStateException("Não é possível anular uma venda que já foi recebida na tesouraria.");
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

        // 🛡️ A CORREÇÃO 1: Mapear a Conta Bancária
        if (v.getContaBancaria() != null) {
            dto.setContaBancariaNome(v.getContaBancaria().getNome());
            // (Nota: Se na tua entidade Venda a variável não for 'contaBancaria' ou o método for 'getDescricao()', ajusta aqui)
        }

        // 🛡️ A CORREÇÃO 2: Mapear o Centro de Custo
        if (v.getCentroCusto() != null) {
            dto.setCentroCustoId(v.getCentroCusto().getId());
            dto.setCentroCustoCodigo(v.getCentroCusto().getCodigo());
        }

        // 🛡️ A CORREÇÃO 3: Mapear a Secção Homogénea
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

                // 🛡️ Não esquecer a designação personalizada
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