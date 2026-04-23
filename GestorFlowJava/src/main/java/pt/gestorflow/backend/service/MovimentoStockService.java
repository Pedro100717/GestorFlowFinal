package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.MovimentoStockDTO;
import pt.gestorflow.backend.dto.MovimentoStockResponseDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.ArtigoRepository;
import pt.gestorflow.backend.repository.MovimentoStockRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MovimentoStockService {

    private final MovimentoStockRepository movimentoRepository;
    private final ArtigoRepository artigoRepository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Transactional
    public MovimentoStockResponseDTO registarAcerto(MovimentoStockDTO dto) {
        Utilizador user = getUtilizadorLogado();

        Artigo artigo = artigoRepository.findByIdAndUtilizadorId(dto.getMercadoriaId(), user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou acesso negado."));

        if (!(artigo instanceof Mercadoria mercadoria)) {
            throw new RuntimeException("Apenas mercadorias possuem controlo de stock.");
        }

        if (dto.getTipo() == MovimentoStock.TipoMovimentoStock.ENTRADA) {
            mercadoria.setStockAtual(mercadoria.getStockAtual().add(dto.getQuantidade()));
        } else {
            mercadoria.setStockAtual(mercadoria.getStockAtual().subtract(dto.getQuantidade()));
        }

        artigoRepository.save(mercadoria);

        MovimentoStock mov = new MovimentoStock();
        mov.setMercadoria(mercadoria);
        mov.setUtilizador(user);
        mov.setTipo(dto.getTipo());
        mov.setQuantidade(dto.getQuantidade());
        mov.setMotivo(dto.getMotivo());
        mov.setDataMovimento(dto.getDataMovimento() != null ? dto.getDataMovimento() : LocalDateTime.now());
        mov.setStockAposMovimento(mercadoria.getStockAtual());

        MovimentoStock movGuardado = movimentoRepository.save(mov);

        return converterParaDTO(movGuardado);
    }

    @Transactional(readOnly = true)
    public MovimentoStockResponseDTO buscarPorId(Long id) {
        Utilizador user = getUtilizadorLogado();

        MovimentoStock movimento = movimentoRepository.findByIdAndUtilizadorId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Movimento de stock não encontrado ou acesso negado."));

        return converterParaDTO(movimento);
    }

    // 🛡️ ATUALIZADO: Agora usa a Query que esconde as compras/vendas
    @Transactional(readOnly = true)
    public Page<MovimentoStockResponseDTO> listarHistorico(int pagina, int tamanho) {
        // Já não precisamos do Sort.by() aqui porque o ORDER BY está no SQL do Repository
        Pageable pageable = PageRequest.of(pagina, tamanho);
        return movimentoRepository.buscarApenasAcertosManuais(getUtilizadorLogado().getId(), pageable)
                .map(this::converterParaDTO);
    }

    // 🛡️ ATUALIZADO: Agora usa a Query que força o filtro pelo artigo exato
    @Transactional(readOnly = true)
    public Page<MovimentoStockResponseDTO> listarPorArtigo(Long artigoId, int pagina, int tamanho) {
        Utilizador user = getUtilizadorLogado();
        // Definimos a ordenação aqui para garantir que o histórico aparece do mais recente para o mais antigo
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("dataMovimento").descending());

        return movimentoRepository.findAllByMercadoriaIdAndUtilizadorId(artigoId, user.getId(), pageable)
                .map(this::converterParaDTO);
    }

    private MovimentoStockResponseDTO converterParaDTO(MovimentoStock m) {
        MovimentoStockResponseDTO dto = new MovimentoStockResponseDTO();
        dto.setId(m.getId());
        dto.setDataMovimento(m.getDataMovimento());
        dto.setTipo(m.getTipo().name());
        dto.setQuantidade(m.getQuantidade());
        dto.setMotivo(m.getMotivo());
        dto.setStockAposMovimento(m.getStockAposMovimento());

        if (m.getMercadoria() != null) {
            dto.setMercadoriaId(m.getMercadoria().getId());
            dto.setMercadoriaNome(m.getMercadoria().getNome());
        }
        return dto;
    }
}