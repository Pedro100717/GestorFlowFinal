package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException; // 🛡️ Import correto para 404
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.*;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.PatrimonioRepository;

@Service
@RequiredArgsConstructor
public class PatrimonioService {

    private final PatrimonioRepository repository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // LISTAR TUDO
    @Transactional(readOnly = true)
    public Page<PatrimonioResponseDTO> listarPatrimonio(Pageable pageable) {
        return repository.findAllByUtilizadorIdAndAtivoTrue(getUtilizadorLogado().getId(), pageable)
                .map(this::mapToDTO);
    }

    // --- CRIAR VIATURA ---
    @Transactional
    public PatrimonioResponseDTO criarViatura(PatrimonioViaturaDTO dto) {
        PatrimonioViatura p = new PatrimonioViatura();
        configurarBasePatrimonio(p, dto.getNome(), dto.getDataAquisicao(), dto.getValorAquisicao());

        p.setMatricula(dto.getMatricula());
        p.setMarca(dto.getMarca());
        p.setModelo(dto.getModelo());
        p.setValidadeSeguro(dto.getValidadeSeguro());
        p.setProximaInspecao(dto.getProximaInspecao());

        return mapToDTO(repository.save(p));
    }

    // --- CRIAR IMÓVEL ---
    @Transactional
    public PatrimonioResponseDTO criarImovel(PatrimonioImovelDTO dto) {
        PatrimonioImovel p = new PatrimonioImovel();
        configurarBasePatrimonio(p, dto.getNome(), dto.getDataAquisicao(), dto.getValorAquisicao());

        p.setMorada(dto.getMorada());
        p.setArtigoMatricial(dto.getArtigoMatricial());
        p.setTipo(dto.getTipo());

        return mapToDTO(repository.save(p));
    }

    // --- CRIAR FERRAMENTA ---
    @Transactional
    public PatrimonioResponseDTO criarFerramenta(PatrimonioFerramentaDTO dto) {
        PatrimonioFerramenta p = new PatrimonioFerramenta();
        configurarBasePatrimonio(p, dto.getNome(), dto.getDataAquisicao(), dto.getValorAquisicao());

        p.setNumeroSerie(dto.getNumeroSerie());
        p.setEstadoConservacao(dto.getEstadoConservacao());

        return mapToDTO(repository.save(p));
    }

    @Transactional(readOnly = true)
    public PatrimonioResponseDTO buscarPorId(Long id) {
        Utilizador user = getUtilizadorLogado();

        // 🛡️ Usamos "repository" em vez de "patrimonioRepository"
        Patrimonio patrimonio = repository.findByIdAndUtilizadorId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Património não encontrado ou acesso negado."));

        // 🛡️ Usamos o teu "mapToDTO" em vez do "converterParaDTO"
        return mapToDTO(patrimonio);
    }

    // --- ELIMINAR (SOFT DELETE) ---
    @Transactional
    public void eliminar(Long id) {
        Utilizador user = getUtilizadorLogado();

        // 🛡️ CORREÇÃO IDOR: Validação direta na base de dados
        Patrimonio p = repository.findByIdAndUtilizadorId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Património não encontrado ou acesso negado."));

        p.setAtivo(false);
        repository.save(p);
    }

    // --- AUXILIAR PARA EVITAR DUPLICAÇÃO ---
    private void configurarBasePatrimonio(Patrimonio p, String nome, java.time.LocalDate data, java.math.BigDecimal valor) {
        p.setNome(nome);
        p.setDataAquisicao(data);
        p.setValorAquisicao(valor);
        p.setUtilizador(getUtilizadorLogado());
        p.setAtivo(true);
    }

    // --- CONVERSOR (MAPPER) ---
    private PatrimonioResponseDTO mapToDTO(Patrimonio p) {
        PatrimonioResponseDTO dto = new PatrimonioResponseDTO();
        dto.setId(p.getId());
        dto.setNome(p.getNome());
        dto.setDataAquisicao(p.getDataAquisicao());
        dto.setValorAquisicao(p.getValorAquisicao());
        dto.setAtivo(p.isAtivo());

        if (p instanceof PatrimonioViatura v) {
            dto.setTipoPatrimonio("VIATURA");
            dto.setMatricula(v.getMatricula());
            dto.setMarca(v.getMarca());
            dto.setModelo(v.getModelo());
            dto.setValidadeSeguro(v.getValidadeSeguro());
            dto.setProximaInspecao(v.getProximaInspecao());
        }
        else if (p instanceof PatrimonioImovel i) {
            dto.setTipoPatrimonio("IMOVEL");
            dto.setMorada(i.getMorada());
            dto.setArtigoMatricial(i.getArtigoMatricial());
            dto.setTipo(i.getTipo());
        }
        else if (p instanceof PatrimonioFerramenta f) {
            dto.setTipoPatrimonio("FERRAMENTA");
            dto.setNumeroSerie(f.getNumeroSerie());
            dto.setEstadoConservacao(f.getEstadoConservacao());
        }
        return dto;
    }
}