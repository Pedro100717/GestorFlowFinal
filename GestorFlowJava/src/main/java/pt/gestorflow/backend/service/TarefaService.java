package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.TarefaDTO;
import pt.gestorflow.backend.model.Cliente;
import pt.gestorflow.backend.model.Tarefa;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.ClienteRepository;
import pt.gestorflow.backend.repository.TarefaRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TarefaService {

    private final TarefaRepository repository;
    private final ClienteRepository clienteRepository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public Tarefa criar(TarefaDTO dto) {
        Utilizador user = getUtilizadorLogado();
        Tarefa t = new Tarefa();

        t.setTitulo(dto.getTitulo());
        t.setDescricao(dto.getDescricao());
        t.setPrioridade(dto.getPrioridade());
        t.setDataLimite(dto.getDataLimite());
        t.setUtilizador(user);

        // Se vier estado, usa, senão PENDENTE
        if (dto.getEstado() != null) t.setEstado(dto.getEstado());

        // Ligar a Cliente (Opcional)
        if (dto.getClienteId() != null) {
            Cliente c = clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), user.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));
            t.setCliente(c);
        }

        return repository.save(t);
    }

    public Tarefa atualizar(Long id, TarefaDTO dto) {
        Tarefa t = repository.findByIdAndUtilizadorId(id, getUtilizadorLogado().getId())
                .orElseThrow(() -> new EntityNotFoundException("Tarefa não encontrada"));

        t.setTitulo(dto.getTitulo());
        t.setDescricao(dto.getDescricao());
        t.setPrioridade(dto.getPrioridade());
        t.setDataLimite(dto.getDataLimite());

        // Gestão inteligente do estado
        if (dto.getEstado() != null) {
            t.setEstado(dto.getEstado());
            // Se passou para concluída, grava a data de hoje automaticamente
            if (dto.getEstado() == Tarefa.EstadoTarefa.CONCLUIDA && t.getDataConclusao() == null) {
                t.setDataConclusao(LocalDateTime.now());
            }
            // Se reabriu a tarefa, limpa a data de conclusão
            else if (dto.getEstado() != Tarefa.EstadoTarefa.CONCLUIDA) {
                t.setDataConclusao(null);
            }
        }

        if (dto.getClienteId() != null) {
            Cliente c = clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), getUtilizadorLogado().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));
            t.setCliente(c);
        } else {
            t.setCliente(null); // Pode desassociar
        }

        return repository.save(t);
    }

    public Page<Tarefa> listarMinhasTarefas(int pagina, int tamanho) {
        // Ordena por prioridade (URGENTE primeiro) e depois por data limite
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("prioridade").descending().and(Sort.by("dataLimite")));
        return repository.findAllByUtilizadorId(getUtilizadorLogado().getId(), pageable);
    }

    public List<Tarefa> listarPorEstado(Tarefa.EstadoTarefa estado) {
        return repository.findAllByUtilizadorIdAndEstado(getUtilizadorLogado().getId(), estado);
    }

    public void eliminar(Long id) {
        Tarefa t = repository.findByIdAndUtilizadorId(id, getUtilizadorLogado().getId())
                .orElseThrow(() -> new EntityNotFoundException("Tarefa não encontrada"));
        repository.delete(t);
    }
}