package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.MovimentoStockDTO;
import pt.gestorflow.backend.dto.MovimentoStockResponseDTO; // NOVO IMPORT
import pt.gestorflow.backend.service.MovimentoStockService;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class MovimentoStockController {

    private final MovimentoStockService service;

    @PostMapping("/acerto")
    public ResponseEntity<MovimentoStockResponseDTO> registarAcerto(@Valid @RequestBody MovimentoStockDTO dto) {
        return ResponseEntity.ok(service.registarAcerto(dto));
    }

    @GetMapping("/historico")
    public ResponseEntity<Page<MovimentoStockResponseDTO>> listarHistorico(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.listarHistorico(page, size));
    }
}