package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional; // Import Correto
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.TarefaDTO;
import pt.gestorflow.backend.dto.TarefaResponseDTO;
import pt.gestorflow.backend.model.Cliente;
import pt.gestorflow.backend.model.Tarefa;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.ClienteRepository;
import pt.gestorflow.backend.repository.TarefaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TarefaService {

    private final TarefaRepository repository;
    private final ClienteRepository clienteRepository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Transactional
    public TarefaResponseDTO criar(TarefaDTO dto) {
        Utilizador user = getUtilizadorLogado();
        Tarefa t = new Tarefa();

        t.setTitulo(dto.getTitulo());
        t.setDescricao(dto.getDescricao());
        t.setPrioridade(dto.getPrioridade());
        t.setDataLimite(dto.getDataLimite());
        t.setUtilizador(user);

        if (dto.getEstado() != null) t.setEstado(dto.getEstado());

        if (dto.getClienteId() != null) {
            Cliente c = clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), user.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));
            t.setCliente(c);
        }

        Tarefa guardada = repository.save(t);
        return converterParaDTO(guardada);
    }

    @Transactional
    public TarefaResponseDTO atualizar(Long id, TarefaDTO dto) {
        Tarefa t = repository.findByIdAndUtilizadorId(id, getUtilizadorLogado().getId())
                .orElseThrow(() -> new EntityNotFoundException("Tarefa não encontrada"));

        t.setTitulo(dto.getTitulo());
        t.setDescricao(dto.getDescricao());
        t.setPrioridade(dto.getPrioridade());
        t.setDataLimite(dto.getDataLimite());

        if (dto.getEstado() != null) {
            t.setEstado(dto.getEstado());
            if (dto.getEstado() == Tarefa.EstadoTarefa.CONCLUIDA && t.getDataConclusao() == null) {
                t.setDataConclusao(LocalDateTime.now());
            } else if (dto.getEstado() != Tarefa.EstadoTarefa.CONCLUIDA) {
                t.setDataConclusao(null);
            }
        }

        if (dto.getClienteId() != null) {
            Cliente c = clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), getUtilizadorLogado().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));
            t.setCliente(c);
        } else {
            t.setCliente(null);
        }

        Tarefa atualizada = repository.save(t);
        return converterParaDTO(atualizada);
    }

    @Transactional(readOnly = true)
    public Page<TarefaResponseDTO> listarMinhasTarefas(int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("prioridade").descending().and(Sort.by("dataLimite")));
        return repository.findAllByUtilizadorId(getUtilizadorLogado().getId(), pageable)
                .map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public List<TarefaResponseDTO> listarPorEstado(Tarefa.EstadoTarefa estado) {
        return repository.findAllByUtilizadorIdAndEstado(getUtilizadorLogado().getId(), estado).stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void eliminar(Long id) {
        Tarefa t = repository.findByIdAndUtilizadorId(id, getUtilizadorLogado().getId())
                .orElseThrow(() -> new EntityNotFoundException("Tarefa não encontrada"));
        repository.delete(t);
    }

    // --- CONVERSOR ---
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