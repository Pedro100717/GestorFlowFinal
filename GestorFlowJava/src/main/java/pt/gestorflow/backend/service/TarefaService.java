package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
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

        // 🛡️ Lógica Defensiva: Garante sempre um estado inicial válido
        if (dto.getEstado() != null) {
            t.setEstado(dto.getEstado());
        } else {
            t.setEstado(Tarefa.EstadoTarefa.PENDENTE); // Ou o teu estado inicial padrão
        }

        if (dto.getClienteId() != null) {
            Cliente c = clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), user.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado ou acesso negado."));
            t.setCliente(c);
        }

        return converterParaDTO(repository.save(t));
    }

    @Transactional
    public TarefaResponseDTO atualizar(Long id, TarefaDTO dto) {
        Utilizador user = getUtilizadorLogado();
        Tarefa t = repository.findByIdAndUtilizadorId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Tarefa não encontrada ou acesso negado."));

        t.setTitulo(dto.getTitulo());
        t.setDescricao(dto.getDescricao());
        t.setPrioridade(dto.getPrioridade());
        t.setDataLimite(dto.getDataLimite());

        // 🛡️ Gestão de Ciclo de Vida: Data de conclusão automática
        if (dto.getEstado() != null) {
            // Se mudar para CONCLUIDA agora, marca a data
            if (dto.getEstado() == Tarefa.EstadoTarefa.CONCLUIDA && t.getEstado() != Tarefa.EstadoTarefa.CONCLUIDA) {
                t.setDataConclusao(LocalDateTime.now());
            }
            // Se reabrir uma tarefa concluída, limpa a data
            else if (dto.getEstado() != Tarefa.EstadoTarefa.CONCLUIDA) {
                t.setDataConclusao(null);
            }
            t.setEstado(dto.getEstado());
        }

        if (dto.getClienteId() != null) {
            Cliente c = clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), user.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado ou acesso negado."));
            t.setCliente(c);
        } else {
            t.setCliente(null);
        }

        return converterParaDTO(repository.save(t));
    }

    @Transactional(readOnly = true)
    public Page<TarefaResponseDTO> listarMinhasTarefas(int pagina, int tamanho) {
        // Ordenação industrial: Primeiro o que é urgente (prioridade), depois o que está perto do prazo
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

    // 🛡️ ADICIONADO: Busca segura de uma tarefa específica
    @Transactional(readOnly = true)
    public TarefaResponseDTO buscarPorId(Long id) {
        Utilizador user = getUtilizadorLogado();

        // 🛡️ PROTEÇÃO IDOR
        Tarefa t = repository.findByIdAndUtilizadorId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Tarefa não encontrada ou acesso negado."));

        return converterParaDTO(t);
    }

    @Transactional
    public void eliminar(Long id) {
        Tarefa t = repository.findByIdAndUtilizadorId(id, getUtilizadorLogado().getId())
                .orElseThrow(() -> new EntityNotFoundException("Tarefa não encontrada ou acesso negado."));
        repository.delete(t);
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