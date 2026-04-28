package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
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

@Service
@RequiredArgsConstructor
public class MovimentoStockService {

    private final MovimentoStockRepository movimentoRepository;
    private final ArtigoRepository artigoRepository;
    private final UtilizadorRepository utilizadorRepository; // 🚀 Necessário para o registo de acerto
    private final AuthService authService; // 🚀 A nossa nova Chave Mestra

    @Transactional
    public MovimentoStockResponseDTO registarAcerto(MovimentoStockDTO dto) {
        // 🚀 1. Obtém o ID blindado do Token
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🚀 2. Busca a entidade física do Utilizador para o log de auditoria
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        // 🛡️ PROTEÇÃO IDOR: Garante que o artigo pertence à empresa do utilizador
        Artigo artigo = artigoRepository.findByIdAndUtilizadorId(dto.getMercadoriaId(), utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou acesso negado."));

        if (!(artigo instanceof Mercadoria mercadoria)) {
            throw new RuntimeException("Apenas mercadorias possuem controlo de stock.");
        }

        // Atualiza o stock na entidade Mercadoria
        if (dto.getTipo() == MovimentoStock.TipoMovimentoStock.ENTRADA) {
            mercadoria.setStockAtual(mercadoria.getStockAtual().add(dto.getQuantidade()));
        } else {
            mercadoria.setStockAtual(mercadoria.getStockAtual().subtract(dto.getQuantidade()));
        }

        artigoRepository.save(mercadoria);

        // Cria o registo de movimento
        MovimentoStock mov = new MovimentoStock();
        mov.setMercadoria(mercadoria);
        mov.setUtilizador(user); // 🚀 Injeção segura da entidade obtida via Token
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
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ PROTEÇÃO IDOR: Impede que espreitem movimentos de stock de outros utilizadores
        MovimentoStock movimento = movimentoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Movimento de stock não encontrado ou acesso negado."));

        return converterParaDTO(movimento);
    }

    @Transactional(readOnly = true)
    public Page<MovimentoStockResponseDTO> listarHistorico(int pagina, int tamanho) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        Pageable pageable = PageRequest.of(pagina, tamanho);
        return movimentoRepository.buscarApenasAcertosManuais(utilizadorId, pageable)
                .map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public Page<MovimentoStockResponseDTO> listarPorArtigo(Long artigoId, int pagina, int tamanho) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

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