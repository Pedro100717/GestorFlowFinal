package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.ArtigoDTO;
import pt.gestorflow.backend.dto.ArtigoResponseDTO; // Importar novo DTO
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArtigoService {

    private final ArtigoRepository artigoRepository;
    private final FamiliaRepository familiaRepository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

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
            Familia familia = familiaRepository.findById(dto.getFamiliaId())
                    .orElseThrow(() -> new EntityNotFoundException("Família não encontrada"));
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

    public ArtigoResponseDTO atualizar(Long id, ArtigoDTO dto) {
        Artigo artigo = artigoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado"));

        artigo.setNome(dto.getNome());
        artigo.setCodigoBarras(dto.getCodigoBarras());

        if (dto.getFamiliaId() != null) {
            Familia familia = familiaRepository.findById(dto.getFamiliaId())
                    .orElseThrow(() -> new EntityNotFoundException("Família não encontrada"));
            artigo.setFamilia(familia);
        } else {
            artigo.setFamilia(null);
        }

        return converterParaDTO(artigoRepository.save(artigo));
    }

    public void eliminar(Long id) {
        if (!artigoRepository.existsById(id)) {
            throw new EntityNotFoundException("Artigo não encontrado");
        }
        artigoRepository.deleteById(id);
    }

    // --- CONVERSOR INTERNO ---
    private ArtigoResponseDTO converterParaDTO(Artigo a) {
        ArtigoResponseDTO dto = new ArtigoResponseDTO();
        dto.setId(a.getId());
        dto.setNome(a.getNome());
        dto.setCodigoBarras(a.getCodigoBarras());
        dto.setPreco(a.getPreco());
        dto.setUltimoPrecoCusto(a.getUltimoPrecoCusto());

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