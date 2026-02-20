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

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Transactional
    public Venda registarVenda(VendaDTO dto) {
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
            // Verificar disponibilidade
            //if (mercadoria.getStockAtual().compareTo(dto.getQuantidade()) < 0) {
                //throw new RuntimeException("Stock insuficiente para o artigo: " + artigo.getNome());
            //}


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

        // 4. Cálculos Financeiros (CORRIGIDO)
        venda.setQuantidade(dto.getQuantidade());

        // CORREÇÃO IMPORTANTE: Usamos o preço que vem do DTO (Input), não do Artigo (que pode ser 0)
        venda.setPrecoUnitario(dto.getPrecoUnitario());

        // Calcular totais usando o preço inserido
        BigDecimal totalSemIva = dto.getPrecoUnitario().multiply(dto.getQuantidade());

        // Calcular IVA (Ex: 100 * 0.23 = 23)
        BigDecimal percentagemIva = taxaIva.getValor().divide(BigDecimal.valueOf(100));
        BigDecimal valorIva = totalSemIva.multiply(percentagemIva);

        // Total Final
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

        return vendaRepository.save(venda);
    }

    public Page<Venda> listarMinhasVendas(int pagina, int tamanho) {
        Utilizador user = getUtilizadorLogado();
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("dataVenda").descending());
        return vendaRepository.findAllByUtilizadorId(user.getId(), pageable);
    }

    public List<TxIva> listarTaxasIva(){
        return txIvaRepository.findAll();
    }
}