package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.*;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.PatrimonioRepository;
import pt.gestorflow.backend.repository.UtilizadorRepository;

@Slf4j // 🚀 Anotação Mágica do Lombok
@Service
@RequiredArgsConstructor
public class PatrimonioService {

    private final PatrimonioRepository repository;
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    // --- MÉTODOS DE BUSCA E LISTAGEM ---

    @Transactional(readOnly = true)
    public Page<PatrimonioResponseDTO> listarPatrimonio(Pageable pageable) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.debug("Listagem de património ativo solicitada pelo utilizador ID: {}", utilizadorId);

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
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Auditoria: O utilizador ID: {} está a registar uma nova Viatura (Matrícula: {})", utilizadorId, dto.getMatricula());

        // 🚀 RESOLUÇÃO DO TEU AVISO: Validação explícita que dispara o Erro 400 no Handler
        if (dto.getMatricula() != null && repository.existsByMatriculaAndUtilizadorId(dto.getMatricula(), utilizadorId)) {
            log.warn("Bloqueada tentativa de registo de viatura com matrícula duplicada ({}) para o utilizador ID: {}", dto.getMatricula(), utilizadorId);
            throw new IllegalArgumentException("Já existe uma viatura registada com esta matrícula.");
        }

        Utilizador user = getUtilizadorSeguro(utilizadorId);
        PatrimonioViatura p = new PatrimonioViatura();

        configurarBasePatrimonio(p, dto.getNome(), dto.getDataAquisicao(), dto.getValorAquisicao(), user);

        p.setMatricula(dto.getMatricula());
        p.setMarca(dto.getMarca());
        p.setModelo(dto.getModelo());
        p.setValidadeSeguro(dto.getValidadeSeguro());
        p.setProximaInspecao(dto.getProximaInspecao());

        PatrimonioViatura salvo = repository.save(p);
        log.debug("Viatura registada com sucesso (ID: {})", salvo.getId());

        return mapToDTO(salvo);
    }

    @Transactional
    public PatrimonioResponseDTO criarImovel(PatrimonioImovelDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Auditoria: O utilizador ID: {} está a registar um novo Imóvel ({})", utilizadorId, dto.getNome());

        Utilizador user = getUtilizadorSeguro(utilizadorId);
        PatrimonioImovel p = new PatrimonioImovel();

        configurarBasePatrimonio(p, dto.getNome(), dto.getDataAquisicao(), dto.getValorAquisicao(), user);

        p.setMorada(dto.getMorada());
        p.setArtigoMatricial(dto.getArtigoMatricial());
        p.setTipo(dto.getTipo());

        return mapToDTO(repository.save(p));
    }

    @Transactional
    public PatrimonioResponseDTO criarFerramenta(PatrimonioFerramentaDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Auditoria: O utilizador ID: {} está a registar uma nova Ferramenta ({})", utilizadorId, dto.getNome());

        Utilizador user = getUtilizadorSeguro(utilizadorId);
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

        log.info("Auditoria Crítica: O utilizador ID: {} pediu a eliminação (Soft Delete) do património ID: {}", utilizadorId, id);

        Patrimonio p = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Património não encontrado ou acesso negado."));

        p.setAtivo(false);
        repository.save(p);

        log.debug("Património ID: {} desativado com sucesso.", id);
    }

    // --- MÉTODOS AUXILIARES E MAPPER ---

    // 🚀 Otimizado para receber o ID já calculado e não ir buscá-lo de novo ao SecurityContext
    private Utilizador getUtilizadorSeguro(Long utilizadorId) {
        return utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado no sistema."));
    }

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