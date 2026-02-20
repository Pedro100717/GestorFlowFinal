package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.OrcamentoDTO;
import pt.gestorflow.backend.model.Orcamento;
import pt.gestorflow.backend.service.OrcamentoService;

@RestController
@RequestMapping("/api/orcamentos")
@RequiredArgsConstructor
public class OrcamentoController {

    private final OrcamentoService service;

    @PostMapping
    public ResponseEntity<Orcamento> criar(@Valid @RequestBody OrcamentoDTO dto) {
        return ResponseEntity.ok(service.criarOrcamento(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Orcamento> atualizar(@PathVariable Long id, @Valid @RequestBody OrcamentoDTO dto) {
        return ResponseEntity.ok(service.atualizarOrcamento(id, dto));
    }

    @GetMapping
    public ResponseEntity<Page<Orcamento>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.listarMeusOrcamentos(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Orcamento> detalhe(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminarOrcamento(id);
        return ResponseEntity.noContent().build();
    }

    // Endpoint rápido para mudar estado (Ex: Cliente aprovou por telefone)
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Orcamento> mudarEstado(
            @PathVariable Long id,
            @RequestParam Orcamento.EstadoOrcamento estado
    ) {
        return ResponseEntity.ok(service.alterarEstado(id, estado));
    }

    @PostMapping("/{id}/converter")
    public ResponseEntity<Void> converterEmVenda(@PathVariable Long id) {
        service.converterEmVenda(id);
        return ResponseEntity.ok().build();
    }
}