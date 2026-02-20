package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.VendaDTO;
import pt.gestorflow.backend.model.TxIva; // Importar
import pt.gestorflow.backend.model.Venda;
import pt.gestorflow.backend.service.VendaService;

import java.util.List;

@RestController
@RequestMapping("/api/vendas")
@RequiredArgsConstructor
public class VendaController {

    private final VendaService service;

    @PostMapping
    public ResponseEntity<?> criar(@Valid @RequestBody VendaDTO dto) {
        try {
            return ResponseEntity.ok(service.registarVenda(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<Page<Venda>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.listarMinhasVendas(page, size));
    }

    // --- NOVO: Endpoint para fornecer as taxas ao Frontend ---
    @GetMapping("/taxas-iva")
    public List<TxIva> listarTaxasIva() {
        return service.listarTaxasIva();
    }
}