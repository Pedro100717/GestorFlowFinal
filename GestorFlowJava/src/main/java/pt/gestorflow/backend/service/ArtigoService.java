package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // 🛡️ ADICIONADO: @Transactional garante que ou tudo grava, ou nada grava (Rollback)
    @Transactional
    public ArtigoResponseDTO criarArtigo(ArtigoDTO dto) {
        Utilizador user = getUtilizadorLogado();
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
            // 🛡️ CORREÇÃO IDOR: Garantir que a Família também pertence ao Utilizador!
            Familia familia = familiaRepository.findByIdAndUtilizadorId(dto.getFamiliaId(), user.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Família não encontrada ou acesso negado."));
            artigo.setFamilia(familia);
        }

        return converterParaDTO(artigoRepository.save(artigo));
    }

    @Transactional(readOnly = true)
    public Page<ArtigoResponseDTO> listarMeusArtigos(int pagina, int tamanho) {
        Utilizador user = getUtilizadorLogado();
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("nome").ascending());
        return artigoRepository.findAllByUtilizadorId(user.getId(), pageable).map(this::converterParaDTO);
    }

    @Transactional
    public ArtigoResponseDTO atualizar(Long id, ArtigoDTO dto) {
        Utilizador user = getUtilizadorLogado();

        // 🛡️ CORREÇÃO IDOR CRÍTICA: Impedir que alguém edite artigos de outra empresa!
        Artigo artigo = artigoRepository.findByIdAndUtilizadorId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou acesso negado."));

        artigo.setNome(dto.getNome());
        artigo.setCodigoBarras(dto.getCodigoBarras());

        if (dto.getFamiliaId() != null) {
            // 🛡️ CORREÇÃO IDOR
            Familia familia = familiaRepository.findByIdAndUtilizadorId(dto.getFamiliaId(), user.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Família não encontrada ou acesso negado."));
            artigo.setFamilia(familia);
        } else {
            artigo.setFamilia(null);
        }

        return converterParaDTO(artigoRepository.save(artigo));
    }

    @Transactional
    public void eliminar(Long id) {
        Utilizador user = getUtilizadorLogado();

        // 🛡️ CORREÇÃO IDOR CRÍTICA: O existsById() antigo permitia apagar os artigos dos outros!
        Artigo artigo = artigoRepository.findByIdAndUtilizadorId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou acesso negado."));

        artigoRepository.delete(artigo);
    }

    @Transactional(readOnly = true)
    public ArtigoResponseDTO buscarPorId(Long id) {
        Utilizador user = getUtilizadorLogado();

        // 🛡️ PROTEÇÃO IDOR CRÍTICA: Garantir que não tentam ler o artigo do "vizinho"
        Artigo artigo = artigoRepository.findByIdAndUtilizadorId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou acesso negado."));

        return converterParaDTO(artigo);
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