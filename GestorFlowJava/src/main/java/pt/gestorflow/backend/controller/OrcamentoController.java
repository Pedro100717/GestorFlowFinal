package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.OrcamentoDTO;
import pt.gestorflow.backend.dto.OrcamentoResponseDTO;
import pt.gestorflow.backend.model.Orcamento; // O uso do Enum da entidade aqui é seguro e perfeitamente aceitável!
import pt.gestorflow.backend.service.OrcamentoService;

@RestController
@RequestMapping("/api/orcamentos")
@RequiredArgsConstructor
public class OrcamentoController {

    private final OrcamentoService service;

    // 🛡️ 201 CREATED: O standard para o momento em que um orçamento nasce
    @PostMapping
    public ResponseEntity<OrcamentoResponseDTO> criar(@Valid @RequestBody OrcamentoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarOrcamento(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrcamentoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody OrcamentoDTO dto) {
        return ResponseEntity.ok(service.atualizarOrcamento(id, dto));
    }

    @GetMapping
    public ResponseEntity<Page<OrcamentoResponseDTO>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        return ResponseEntity.ok(service.listarMeusOrcamentos(pagina, tamanho));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrcamentoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // 🛡️ 204 NO CONTENT: Certinho.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminarOrcamento(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<OrcamentoResponseDTO> alterarEstado(
            @PathVariable Long id,
            @RequestParam Orcamento.EstadoOrcamento estado) {
        return ResponseEntity.ok(service.alterarEstado(id, estado));
    }

    // 🛡️ Mantém-se 200 OK. Como é uma ação /RPC style (Remote Procedure Call), o 200 é perfeito.
    @PostMapping("/{id}/converter-venda")
    public ResponseEntity<Void> converterEmVenda(
            @PathVariable Long id,
            @RequestParam Long contaBancariaId) {
        service.converterEmVenda(id, contaBancariaId);
        return ResponseEntity.ok().build();
    }
}