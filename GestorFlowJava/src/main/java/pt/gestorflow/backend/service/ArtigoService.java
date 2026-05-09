package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // <-- CRÍTICO
import pt.gestorflow.backend.dto.ArtigoDTO;
import pt.gestorflow.backend.dto.ArtigoResponseDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ArtigoService {

    private final ArtigoRepository artigoRepository;
    private final FamiliaRepository familiaRepository;
    private final UtilizadorRepository utilizadorRepository; // 🚀 Injetado para ir buscar o objeto Utilizador
    private final AuthService authService; // 🚀 A nossa Chave Mestra

    @Transactional
    public ArtigoResponseDTO criarArtigo(ArtigoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🚀 Vai buscar a entidade do Utilizador em segurança
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        Artigo artigo;

        if (Boolean.TRUE.equals(dto.getMovimentaStock())) {
            Mercadoria m = new Mercadoria();
            m.setStockAtual(BigDecimal.ZERO);
            artigo = m;
        } else {
            artigo = new Servico();
        }

        artigo.setNome(dto.getNome());
        artigo.setCodigoBarras(dto.getCodigoBarras());
        artigo.setPreco(BigDecimal.ZERO);
        artigo.setUltimoPrecoCusto(BigDecimal.ZERO);
        artigo.setUtilizador(user);

        if (dto.getFamiliaId() != null) {
            // 🛡️ Mantida a tua excelente proteção IDOR!
            Familia familia = familiaRepository.findByIdAndUtilizadorId(dto.getFamiliaId(), utilizadorId)
                    .orElseThrow(() -> new EntityNotFoundException("Família não encontrada ou acesso negado."));
            artigo.setFamilia(familia);
        }

        return converterParaDTO(artigoRepository.save(artigo));
    }

    @Transactional(readOnly = true)
    public Page<ArtigoResponseDTO> listarMeusArtigos(int pagina, int tamanho) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("nome").ascending());
        return artigoRepository.findAllByUtilizadorId(utilizadorId, pageable).map(this::converterParaDTO);
    }

    @Transactional
    public ArtigoResponseDTO atualizar(Long id, ArtigoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ CORREÇÃO IDOR CRÍTICA MANTIDA
        Artigo artigo = artigoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou acesso negado."));

        artigo.setNome(dto.getNome());
        artigo.setCodigoBarras(dto.getCodigoBarras());

        if (dto.getFamiliaId() != null) {
            Familia familia = familiaRepository.findByIdAndUtilizadorId(dto.getFamiliaId(), utilizadorId)
                    .orElseThrow(() -> new EntityNotFoundException("Família não encontrada ou acesso negado."));
            artigo.setFamilia(familia);
        } else {
            artigo.setFamilia(null);
        }

        return converterParaDTO(artigoRepository.save(artigo));
    }

    @Transactional
    public void eliminar(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ CORREÇÃO IDOR CRÍTICA MANTIDA
        Artigo artigo = artigoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou acesso negado."));

        artigoRepository.delete(artigo);
    }

    @Transactional(readOnly = true)
    public ArtigoResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ PROTEÇÃO IDOR CRÍTICA MANTIDA
        Artigo artigo = artigoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou acesso negado."));

        return converterParaDTO(artigo);
    }

    @Transactional
    public void adicionarStock(Long artigoId, BigDecimal quantidade) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        Artigo artigo = artigoRepository.findByIdAndUtilizadorId(artigoId, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou acesso negado."));

        // Só mexemos no stock se for fisicamente palpável (Mercadoria)
        if (artigo instanceof Mercadoria m) {
            m.setStockAtual(m.getStockAtual().add(quantidade));
            artigoRepository.save(m);
        }
    }

    @Transactional
    public void removerStock(Long artigoId, BigDecimal quantidade) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        Artigo artigo = artigoRepository.findByIdAndUtilizadorId(artigoId, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou acesso negado."));

        if (artigo instanceof Mercadoria m) {
            // Em ERPs estritos, poderíamos bloquear se o stock ficasse negativo.
            // Para já, permitimos stock negativo (venda a descoberto), mas atualizamos a matemática:
            m.setStockAtual(m.getStockAtual().subtract(quantidade));
            artigoRepository.save(m);
        }
    }

    // --- CONVERSOR INTERNO ---
    private ArtigoResponseDTO converterParaDTO(Artigo a) {
        ArtigoResponseDTO dto = new ArtigoResponseDTO();
        dto.setId(a.getId());
        dto.setNome(a.getNome());
        dto.setCodigoBarras(a.getCodigoBarras());
        dto.setPreco(a.getPreco());
        dto.setUltimoPrecoCusto(a.getUltimoPrecoCusto());
        dto.setMovimentaStock(a.isMovimentaStock());

        // Identificar tipo e stock
        if (a instanceof Mercadoria m) {
            dto.setTipo("MERCADORIA");
            dto.setStockAtual(m.getStockAtual());
        } else {
            dto.setTipo("SERVICO");
            dto.setStockAtual(null);
        }

        if (a.getFamilia() != null) {
            dto.setFamiliaId(a.getFamilia().getId());
            dto.setFamiliaNome(a.getFamilia().getNome());
        }

        return dto;
    }
}