package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.CompraDTO;
import pt.gestorflow.backend.model.Compra;
import pt.gestorflow.backend.model.TxIva; // <--- Importar
import pt.gestorflow.backend.service.CompraService;

import java.util.List;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService service;

    @PostMapping
    public ResponseEntity<Compra> registarCompra(@Valid @RequestBody CompraDTO dto) {
        return ResponseEntity.ok(service.registarCompra(dto));
    }

    @GetMapping
    public ResponseEntity<Page<Compra>> listarCompras(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.listarMinhasCompras(page, size));
    }

    // --- NOVO ENDPOINT ---
    @GetMapping("/taxas-iva")
    public List<TxIva> listarTaxasIva() {
        return service.listarTaxasIva();
    }
}