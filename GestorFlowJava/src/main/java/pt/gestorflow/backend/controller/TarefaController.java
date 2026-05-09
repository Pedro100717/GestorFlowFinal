package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.TarefaDTO;
import pt.gestorflow.backend.dto.TarefaResponseDTO;
import pt.gestorflow.backend.model.Tarefa;
import pt.gestorflow.backend.service.TarefaService;

import java.util.List;

@RestController
@RequestMapping("/api/tarefas")
@RequiredArgsConstructor
public class TarefaController {

    private final TarefaService tarefaService;

    // 1. Criar Nova Tarefa
    @PostMapping
    public ResponseEntity<TarefaResponseDTO> criar(@Valid @RequestBody TarefaDTO dto) {
        TarefaResponseDTO novaTarefa = tarefaService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaTarefa);
    }

    // 2. Atualizar Tarefa Existente
    @PutMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody TarefaDTO dto) {
        TarefaResponseDTO tarefaAtualizada = tarefaService.atualizar(id, dto);
        return ResponseEntity.ok(tarefaAtualizada);
    }

    // 3. Listar as Minhas Tarefas (Com Paginação à Prova de Bala)
    @GetMapping
    public ResponseEntity<Page<TarefaResponseDTO>> listarMinhasTarefas(
            // 🚀 defaultValues impedem o Erro 500 caso o Angular não envie os parâmetros!
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {

        Page<TarefaResponseDTO> tarefas = tarefaService.listarMinhasTarefas(pagina, tamanho);
        return ResponseEntity.ok(tarefas);
    }

    // 4. Listar Tarefas por Estado (Filtro)
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<TarefaResponseDTO>> listarPorEstado(@PathVariable Tarefa.EstadoTarefa estado) {
        List<TarefaResponseDTO> tarefas = tarefaService.listarPorEstado(estado);
        return ResponseEntity.ok(tarefas);
    }

    // 5. Ver Detalhes de uma Tarefa Específica
    @GetMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> buscarPorId(@PathVariable Long id) {
        TarefaResponseDTO tarefa = tarefaService.buscarPorId(id);
        return ResponseEntity.ok(tarefa);
    }

    // 6. Eliminar Tarefa
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tarefaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}