package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.TarefaDTO;
import pt.gestorflow.backend.dto.TarefaResponseDTO;
import pt.gestorflow.backend.model.Cliente;
import pt.gestorflow.backend.model.Tarefa;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.ClienteRepository;
import pt.gestorflow.backend.repository.TarefaRepository;
import pt.gestorflow.backend.repository.UtilizadorRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j // 🚀 Anotação Mágica do Lombok
@Service
@RequiredArgsConstructor
public class TarefaService {

    private final TarefaRepository repository;
    private final ClienteRepository clienteRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    @Transactional
    public TarefaResponseDTO criar(TarefaDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("A criar nova Tarefa ('{}') para o utilizador ID: {}", dto.getTitulo(), utilizadorId);

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        Tarefa t = new Tarefa();
        t.setTitulo(dto.getTitulo());
        t.setDescricao(dto.getDescricao());
        t.setPrioridade(dto.getPrioridade());
        t.setDataLimite(dto.getDataLimite());
        t.setUtilizador(user);

        Tarefa.EstadoTarefa estadoAtribuido = dto.getEstado() != null ? dto.getEstado() : Tarefa.EstadoTarefa.PENDENTE;
        t.setEstado(estadoAtribuido);

        // 🚀 CORREÇÃO: Se a tarefa já nascer concluída, regista a data de imediato!
        if (estadoAtribuido == Tarefa.EstadoTarefa.CONCLUIDA) {
            t.setDataConclusao(LocalDateTime.now());
        }

        if (dto.getClienteId() != null) {
            Cliente c = clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), utilizadorId)
                    .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado ou acesso negado."));
            t.setCliente(c);
        }

        Tarefa salva = repository.save(t);
        log.debug("Tarefa criada com sucesso com o ID: {}", salva.getId());

        return converterParaDTO(salva);
    }

    @Transactional
    public TarefaResponseDTO atualizar(Long id, TarefaDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("A atualizar a Tarefa ID: {} (Utilizador ID: {})", id, utilizadorId);

        Tarefa t = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Tarefa não encontrada ou acesso negado."));

        t.setTitulo(dto.getTitulo());
        t.setDescricao(dto.getDescricao());
        t.setPrioridade(dto.getPrioridade());
        t.setDataLimite(dto.getDataLimite());

        if (dto.getEstado() != null) {
            if (dto.getEstado() == Tarefa.EstadoTarefa.CONCLUIDA && t.getEstado() != Tarefa.EstadoTarefa.CONCLUIDA) {
                t.setDataConclusao(LocalDateTime.now());
                log.debug("Tarefa ID: {} marcada como CONCLUÍDA.", id);
            } else if (dto.getEstado() != Tarefa.EstadoTarefa.CONCLUIDA) {
                t.setDataConclusao(null);
            }
            t.setEstado(dto.getEstado());
        }

        if (dto.getClienteId() != null) {
            Cliente c = clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), utilizadorId)
                    .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado ou acesso negado."));
            t.setCliente(c);
        } else {
            t.setCliente(null);
        }

        return converterParaDTO(repository.save(t));
    }

    @Transactional(readOnly = true)
    public Page<TarefaResponseDTO> listarMinhasTarefas(int pagina, int tamanho) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.debug("Listagem paginada de tarefas solicitada pelo utilizador ID: {}", utilizadorId);

        Pageable pageable = PageRequest.of(pagina, tamanho,
                Sort.by("prioridade").descending()
                        .and(Sort.by("dataLimite").ascending())
                        .and(Sort.by("id").descending()));

        return repository.findAllByUtilizadorId(utilizadorId, pageable)
                .map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public List<TarefaResponseDTO> listarPorEstado(Tarefa.EstadoTarefa estado) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.debug("Listagem de tarefas pelo estado {} solicitada pelo utilizador ID: {}", estado, utilizadorId);

        return repository.findAllByUtilizadorIdAndEstado(utilizadorId, estado).stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TarefaResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        Tarefa t = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Tarefa não encontrada ou acesso negado."));

        return converterParaDTO(t);
    }

    @Transactional
    public void eliminar(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("O utilizador ID: {} pediu a eliminação da Tarefa ID: {}", utilizadorId, id);

        Tarefa t = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Tarefa não encontrada ou acesso negado."));

        repository.delete(t);
        log.debug("Tarefa ID: {} eliminada com sucesso.", id);
    }

    private TarefaResponseDTO converterParaDTO(Tarefa t) {
        TarefaResponseDTO dto = new TarefaResponseDTO();
        dto.setId(t.getId());
        dto.setTitulo(t.getTitulo());
        dto.setDescricao(t.getDescricao());
        dto.setPrioridade(t.getPrioridade().name());
        dto.setEstado(t.getEstado().name());
        dto.setDataLimite(t.getDataLimite());
        dto.setDataCriacao(t.getDataCriacaoSistema());
        dto.setDataConclusao(t.getDataConclusao());

        if (t.getCliente() != null) {
            dto.setClienteId(t.getCliente().getId());
            dto.setClienteNome(t.getCliente().getNome());
        }
        return dto;
    }
}