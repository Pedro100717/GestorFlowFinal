package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // 🚀 Documentação OpenAPI
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.TarefaDTO;
import pt.gestorflow.backend.dto.TarefaResponseDTO;
import pt.gestorflow.backend.model.Tarefa;
import pt.gestorflow.backend.service.TarefaService;

import java.util.List;

@Slf4j // 🚀 Telemetria ativada
@RestController
@RequestMapping("/api/tarefas")
@RequiredArgsConstructor
@Tag(name = "Tarefas e Produtividade", description = "Gestão de tarefas, lembretes e organização diária do utilizador")
public class TarefaController {

    private final TarefaService tarefaService;

    // 1. Criar Nova Tarefa
    @Operation(summary = "Criar nova Tarefa", description = "Regista uma nova tarefa ou lembrete para o utilizador autenticado.")
    @PostMapping
    public ResponseEntity<TarefaResponseDTO> criar(@Valid @RequestBody TarefaDTO dto) {
        log.info("Produtividade: Nova tarefa criada (Título: {})", dto.getTitulo()); // Assumindo que o DTO tem getTitulo()

        TarefaResponseDTO novaTarefa = tarefaService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaTarefa);
    }

    // 2. Atualizar Tarefa Existente
    @Operation(summary = "Atualizar Tarefa", description = "Edita os dados de uma tarefa (ex: alterar título, data limite ou estado).")
    @PutMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody TarefaDTO dto) {
        log.debug("Pedido HTTP PUT recebido: /api/tarefas/{}", id);

        TarefaResponseDTO tarefaAtualizada = tarefaService.atualizar(id, dto);
        return ResponseEntity.ok(tarefaAtualizada);
    }

    // 3. Listar as Minhas Tarefas (Com Paginação à Prova de Bala)
    @Operation(summary = "Listar todas as Tarefas", description = "Devolve a lista paginada de tarefas do utilizador, independentemente do estado.")
    @GetMapping
    public ResponseEntity<Page<TarefaResponseDTO>> listarMinhasTarefas(
            // 🚀 defaultValues impedem o Erro 500 caso o Angular não envie os parâmetros!
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {

        log.debug("Pedido HTTP GET recebido: /api/tarefas (Página: {}, Tamanho: {})", pagina, tamanho);

        Page<TarefaResponseDTO> tarefas = tarefaService.listarMinhasTarefas(pagina, tamanho);
        return ResponseEntity.ok(tarefas);
    }

    // 4. Listar Tarefas por Estado (Filtro)
    @Operation(summary = "Filtrar Tarefas por Estado", description = "Devolve a lista de tarefas filtrada por um estado específico (ex: PENDENTE, EM_CURSO, CONCLUIDA).")
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<TarefaResponseDTO>> listarPorEstado(@PathVariable Tarefa.EstadoTarefa estado) {
        log.debug("Pedido HTTP GET recebido: /api/tarefas/estado/{}", estado);

        List<TarefaResponseDTO> tarefas = tarefaService.listarPorEstado(estado);
        return ResponseEntity.ok(tarefas);
    }

    // 5. Ver Detalhes de uma Tarefa Específica
    @Operation(summary = "Obter detalhe da Tarefa", description = "Devolve a informação completa de uma tarefa específica pelo seu ID.")
    @GetMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> buscarPorId(@PathVariable Long id) {
        log.debug("Pedido HTTP GET recebido: /api/tarefas/{}", id);

        TarefaResponseDTO tarefa = tarefaService.buscarPorId(id);
        return ResponseEntity.ok(tarefa);
    }

    // 6. Eliminar Tarefa
    @Operation(summary = "Eliminar Tarefa", description = "Apaga permanentemente uma tarefa do sistema.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("Produtividade: Tarefa ID: {} eliminada pelo utilizador.", id);

        tarefaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}