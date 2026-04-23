package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.VendaDTO;
import pt.gestorflow.backend.dto.VendaResponseDTO;
import pt.gestorflow.backend.service.VendaService;
import pt.gestorflow.backend.model.TxIva; // 🛡️ Importação Adicionada

import java.util.List; // 🛡️ Importação Adicionada

@RestController
@RequestMapping("/api/vendas")
@RequiredArgsConstructor
public class VendaController {

    private final VendaService service;

    // 🛡️ 201 CREATED: O Standard
    @PostMapping
    public ResponseEntity<VendaResponseDTO> criar(@Valid @RequestBody VendaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registarVenda(dto));
    }

    // 🛡️ ADICIONADO: Endpoint para as taxas de IVA
    @GetMapping("/taxas-iva")
    public ResponseEntity<List<TxIva>> listarTaxasIva() {
        return ResponseEntity.ok(service.listarTaxasIva());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<Page<VendaResponseDTO>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.listarMinhasVendas(page, size));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anular(@PathVariable Long id) {
        service.anularVenda(id);
        return ResponseEntity.noContent().build();
    }
}