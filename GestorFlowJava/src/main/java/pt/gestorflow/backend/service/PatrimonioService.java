package pt.gestorflow.backend.service;

import org.springframework.transaction.annotation.Transactional; // Import Correto
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
        p.setNome(dto.getNome());
        p.setDataAquisicao(dto.getDataAquisicao());
        p.setValorAquisicao(dto.getValorAquisicao());
        p.setUtilizador(getUtilizadorLogado());

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
        p.setNome(dto.getNome());
        p.setDataAquisicao(dto.getDataAquisicao());
        p.setValorAquisicao(dto.getValorAquisicao());
        p.setUtilizador(getUtilizadorLogado());

        p.setMorada(dto.getMorada());
        p.setArtigoMatricial(dto.getArtigoMatricial());
        p.setTipo(dto.getTipo());

        return mapToDTO(repository.save(p));
    }

    // --- CRIAR FERRAMENTA ---
    @Transactional
    public PatrimonioResponseDTO criarFerramenta(PatrimonioFerramentaDTO dto) {
        PatrimonioFerramenta p = new PatrimonioFerramenta();
        p.setNome(dto.getNome());
        p.setDataAquisicao(dto.getDataAquisicao());
        p.setValorAquisicao(dto.getValorAquisicao());
        p.setUtilizador(getUtilizadorLogado());

        p.setNumeroSerie(dto.getNumeroSerie());
        p.setEstadoConservacao(dto.getEstadoConservacao());

        return mapToDTO(repository.save(p));
    }

    // --- ELIMINAR (SOFT DELETE) ---
    @Transactional
    public void eliminar(Long id) {
        Patrimonio p = repository.findById(id).orElseThrow(() -> new RuntimeException("Património não encontrado"));
        if(!p.getUtilizador().getId().equals(getUtilizadorLogado().getId())) {
            throw new RuntimeException("Acesso negado");
        }

        p.setAtivo(false);
        repository.save(p);
    }

    // --- CONVERSOR (MAPPER) MÁGICO PARA DTO ---
    private PatrimonioResponseDTO mapToDTO(Patrimonio p) {
        PatrimonioResponseDTO dto = new PatrimonioResponseDTO();
        dto.setId(p.getId());
        dto.setNome(p.getNome());
        dto.setDataAquisicao(p.getDataAquisicao());
        dto.setValorAquisicao(p.getValorAquisicao());

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