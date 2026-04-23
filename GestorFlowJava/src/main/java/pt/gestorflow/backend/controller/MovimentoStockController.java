package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.MovimentoStockDTO;
import pt.gestorflow.backend.dto.MovimentoStockResponseDTO;
import pt.gestorflow.backend.service.MovimentoStockService;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class MovimentoStockController {

    private final MovimentoStockService service;

    // 🛡️ 201 CREATED: O padrão correto para novos registos na BD
    @PostMapping("/acerto")
    public ResponseEntity<MovimentoStockResponseDTO> registarAcerto(@Valid @RequestBody MovimentoStockDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registarAcerto(dto));
    }

    // 🛡️ ADICIONADO: Essencial para o Angular abrir o detalhe de um movimento específico
    @GetMapping("/historico/{id}")
    public ResponseEntity<MovimentoStockResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/historico")
    public ResponseEntity<Page<MovimentoStockResponseDTO>> listarHistorico(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.listarHistorico(page, size));
    }

    @GetMapping("/artigo/{artigoId}")
    public ResponseEntity<Page<MovimentoStockResponseDTO>> listarHistoricoDoArtigo(
            @PathVariable Long artigoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // (Nota: Terás de criar o método listarPorArtigo no teu MovimentoStockService)
        return ResponseEntity.ok(service.listarPorArtigo(artigoId, page, size));
    }
}