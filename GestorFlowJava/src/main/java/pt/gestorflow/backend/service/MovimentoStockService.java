package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.MovimentoStockDTO;
import pt.gestorflow.backend.dto.MovimentoStockResponseDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.ArtigoRepository;
import pt.gestorflow.backend.repository.MovimentoStockRepository;
import pt.gestorflow.backend.repository.UtilizadorRepository;

import java.time.LocalDateTime;

@Slf4j // 🚀 Anotação Mágica do Lombok
@Service
@RequiredArgsConstructor
public class MovimentoStockService {

    private final MovimentoStockRepository movimentoRepository;
    private final ArtigoRepository artigoRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;
    private final pt.gestorflow.backend.repository.ChaveIdempotenciaRepository chaveIdempotenciaRepository;

    @Transactional
    public MovimentoStockResponseDTO registarAcerto(MovimentoStockDTO dto, String idempotencyKey) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Auditoria: O utilizador ID: {} está a tentar um acerto manual de stock ({}) para o artigo ID: {}. Quantidade: {}. Chave: {}",
                utilizadorId, dto.getTipo(), dto.getMercadoriaId(), dto.getQuantidade(), idempotencyKey);

        // =========================================================================
        // 🚀 DEFESA: BLOQUEIO ATÓMICO NA BASE DE DADOS (Contra o Duplo Clique)
        // =========================================================================
        try {
            ChaveIdempotencia chave = new ChaveIdempotencia(idempotencyKey, utilizadorId, LocalDateTime.now());
            chaveIdempotenciaRepository.saveAndFlush(chave);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("BLOQUEIO ATÓMICO STOCK: Tentativa de acerto de stock duplicada detetada. Chave: {}", idempotencyKey);
            throw new IllegalStateException("Este acerto de stock já se encontra em processamento ou foi concluído.");
        }

        // =========================================================================
        // O FLUXO DE NEGÓCIO SEGURO
        // =========================================================================
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        Artigo artigo = artigoRepository.findByIdAndUtilizadorId(dto.getMercadoriaId(), utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou acesso negado."));

        if (!(artigo instanceof Mercadoria mercadoria)) {
            log.warn("Bloqueada tentativa de acerto de stock num Serviço (Artigo ID: {}) pelo Utilizador ID: {}", dto.getMercadoriaId(), utilizadorId);
            throw new IllegalArgumentException("Apenas mercadorias possuem controlo de stock.");
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

        log.debug("Acerto manual registado com sucesso (Movimento ID: {}). Novo stock: {}", movGuardado.getId(), mercadoria.getStockAtual());

        return converterParaDTO(movGuardado);
    }

    @Transactional(readOnly = true)
    public MovimentoStockResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        MovimentoStock movimento = movimentoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Movimento de stock não encontrado ou acesso negado."));

        return converterParaDTO(movimento);
    }

    @Transactional(readOnly = true)
    public Page<MovimentoStockResponseDTO> listarHistorico(int pagina, int tamanho) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.debug("Listagem de histórico de acertos manuais solicitada pelo utilizador ID: {}. Página: {}", utilizadorId, pagina);

        Pageable pageable = PageRequest.of(pagina, tamanho);
        return movimentoRepository.buscarApenasAcertosManuais(utilizadorId, pageable)
                .map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public Page<MovimentoStockResponseDTO> listarPorArtigo(Long artigoId, int pagina, int tamanho) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.debug("Listagem de movimentos do artigo ID: {} solicitada pelo utilizador ID: {}. Página: {}", artigoId, utilizadorId, pagina);

        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("dataMovimento").descending());

        return movimentoRepository.findAllByMercadoriaIdAndUtilizadorId(artigoId, utilizadorId, pageable)
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