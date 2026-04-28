package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.*;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.PatrimonioRepository;
import pt.gestorflow.backend.repository.UtilizadorRepository;

@Service
@RequiredArgsConstructor
public class PatrimonioService {

    private final PatrimonioRepository repository;
    private final UtilizadorRepository utilizadorRepository; // 🚀 Necessário para associar às novas entidades
    private final AuthService authService; // 🚀 A nossa Chave Mestra

    // --- MÉTODOS DE BUSCA E LISTAGEM ---

    @Transactional(readOnly = true)
    public Page<PatrimonioResponseDTO> listarPatrimonio(Pageable pageable) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        return repository.findAllByUtilizadorIdAndAtivoTrue(utilizadorId, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public PatrimonioResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        Patrimonio patrimonio = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Património não encontrado ou acesso negado."));

        return mapToDTO(patrimonio);
    }

    // --- MÉTODOS DE CRIAÇÃO ---

    @Transactional
    public PatrimonioResponseDTO criarViatura(PatrimonioViaturaDTO dto) {
        Utilizador user = getUtilizadorSeguro();
        PatrimonioViatura p = new PatrimonioViatura();

        // Passamos o user diretamente para não ir buscá-lo 2 vezes
        configurarBasePatrimonio(p, dto.getNome(), dto.getDataAquisicao(), dto.getValorAquisicao(), user);

        // ⚠️ CRÍTICA CONSTRUTIVA: No futuro, deves validar aqui se a matrícula já existe,
        // porque a tua base de dados tem um "UNIQUE" constraint nesta coluna.
        p.setMatricula(dto.getMatricula());
        p.setMarca(dto.getMarca());
        p.setModelo(dto.getModelo());
        p.setValidadeSeguro(dto.getValidadeSeguro());
        p.setProximaInspecao(dto.getProximaInspecao());

        return mapToDTO(repository.save(p));
    }

    @Transactional
    public PatrimonioResponseDTO criarImovel(PatrimonioImovelDTO dto) {
        Utilizador user = getUtilizadorSeguro();
        PatrimonioImovel p = new PatrimonioImovel();
        configurarBasePatrimonio(p, dto.getNome(), dto.getDataAquisicao(), dto.getValorAquisicao(), user);

        p.setMorada(dto.getMorada());
        p.setArtigoMatricial(dto.getArtigoMatricial());
        p.setTipo(dto.getTipo());

        return mapToDTO(repository.save(p));
    }

    @Transactional
    public PatrimonioResponseDTO criarFerramenta(PatrimonioFerramentaDTO dto) {
        Utilizador user = getUtilizadorSeguro();
        PatrimonioFerramenta p = new PatrimonioFerramenta();
        configurarBasePatrimonio(p, dto.getNome(), dto.getDataAquisicao(), dto.getValorAquisicao(), user);

        p.setNumeroSerie(dto.getNumeroSerie());
        p.setEstadoConservacao(dto.getEstadoConservacao());

        return mapToDTO(repository.save(p));
    }

    // --- ELIMINAR (SOFT DELETE) ---

    @Transactional
    public void eliminar(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ PROTEÇÃO IDOR: Validação de dono antes do soft delete
        Patrimonio p = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Património não encontrado ou acesso negado."));

        p.setAtivo(false);
        repository.save(p);
    }

    // --- MÉTODOS AUXILIARES E MAPPER ---

    // 🚀 Extrai o utilizador da BD garantindo a identidade através do Token
    private Utilizador getUtilizadorSeguro() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        return utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado no sistema."));
    }

    // Agora recebe o Utilizador por parâmetro em vez de o tentar ir adivinhar
    private void configurarBasePatrimonio(Patrimonio p, String nome, java.time.LocalDate data, java.math.BigDecimal valor, Utilizador user) {
        p.setNome(nome);
        p.setDataAquisicao(data);
        p.setValorAquisicao(valor);
        p.setUtilizador(user);
        p.setAtivo(true);
    }

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