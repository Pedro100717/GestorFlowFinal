package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.CompraDTO;
import pt.gestorflow.backend.dto.CompraResponseDTO; // <--- IMPORT DO DTO PLANO
import pt.gestorflow.backend.model.TxIva;
import pt.gestorflow.backend.service.CompraService;

import java.util.List;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService service;

    @PostMapping
    public ResponseEntity<?> criar(@Valid @RequestBody CompraDTO dto) {
        try {
            return ResponseEntity.ok(service.registarCompra(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<Page<CompraResponseDTO>> listar( // <--- CORRIGIDO AQUI PARA O DTO
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.listarMinhasCompras(page, size));
    }

    @GetMapping("/taxas-iva")
    public List<TxIva> listarTaxasIva() {
        return service.listarTaxasIva();
    }
}