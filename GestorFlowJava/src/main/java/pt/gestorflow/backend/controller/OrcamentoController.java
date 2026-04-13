package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.OrcamentoDTO;
import pt.gestorflow.backend.dto.OrcamentoResponseDTO;
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

    // 🛡️ CORREÇÃO: Agora espera Page<OrcamentoResponseDTO>
    @GetMapping
    public ResponseEntity<Page<OrcamentoResponseDTO>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        return ResponseEntity.ok(service.listarMeusOrcamentos(pagina, tamanho));
    }

    // 🛡️ CORREÇÃO: Agora espera OrcamentoResponseDTO
    @GetMapping("/{id}")
    public ResponseEntity<OrcamentoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminarOrcamento(id);
        return ResponseEntity.noContent().build();
    }

    // 🛡️ CORREÇÃO: Agora espera OrcamentoResponseDTO
    @PatchMapping("/{id}/estado")
    public ResponseEntity<OrcamentoResponseDTO> alterarEstado(
            @PathVariable Long id,
            @RequestParam Orcamento.EstadoOrcamento estado) {
        return ResponseEntity.ok(service.alterarEstado(id, estado));
    }

    @PostMapping("/{id}/converter-venda")
    public ResponseEntity<Void> converterEmVenda(
            @PathVariable Long id,
            @RequestParam Long contaBancariaId) {
        service.converterEmVenda(id, contaBancariaId);
        return ResponseEntity.ok().build();
    }
}