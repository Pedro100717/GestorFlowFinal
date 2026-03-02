package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.VendaDTO;
import pt.gestorflow.backend.dto.VendaResponseDTO; // <--- NOVO IMPORT
import pt.gestorflow.backend.model.TxIva;
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
    public ResponseEntity<Page<VendaResponseDTO>> listar( // <--- CORRIGIDO AQUI PARA O DTO
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.listarMinhasVendas(page, size));
    }

    @GetMapping("/taxas-iva")
    public List<TxIva> listarTaxasIva() {
        return service.listarTaxasIva();
    }
}