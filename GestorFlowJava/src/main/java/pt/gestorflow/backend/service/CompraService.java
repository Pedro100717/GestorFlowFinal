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
import pt.gestorflow.backend.dto.CompraDTO;
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

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Transactional
    public Compra registarCompra(CompraDTO dto) {
        Utilizador user = getUtilizadorLogado();

        Fornecedor fornecedor = fornecedorRepository.findById(dto.getFornecedorId())
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado"));

        Artigo artigo = artigoRepository.findById(dto.getArtigoId())
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado"));

        // 1. Buscar Taxa de IVA (Obrigatória na compra)
        TxIva taxaIva = txIvaRepository.findById(dto.getTaxaIvaId())
                .orElseThrow(() -> new EntityNotFoundException("Taxa de IVA não encontrada"));

        // 2. Lógica de Stock e Custo

        // NOVO: Verifica se é Mercadoria antes de mexer no stock!
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
        compra.setTaxaIva(taxaIva); // Guardamos a taxa usada nesta compra

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

        return compraRepository.save(compra);
    }

    public Page<Compra> listarMinhasCompras(int pagina, int tamanho) {
        Utilizador user = getUtilizadorLogado();
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("dataCompra").descending());
        return compraRepository.findAllByUtilizadorId(user.getId(), pageable);
    }

    public List<TxIva> listarTaxasIva() {
        return txIvaRepository.findAll();
    }
}