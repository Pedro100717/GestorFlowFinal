package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.CompraDTO;
import pt.gestorflow.backend.dto.CompraResponseDTO;
import pt.gestorflow.backend.model.TxIva; // (Lê a nota abaixo sobre isto!)
import pt.gestorflow.backend.service.CompraService;

import java.util.List;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService service;

    // 🛡️ CONTRATO DE AÇO: Fim do try-catch e do <?>. Retorna 201 Created.
    @PostMapping
    public ResponseEntity<CompraResponseDTO> criar(@Valid @RequestBody CompraDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registarCompra(dto));
    }

    // 🛡️ ADICIONADO: O Angular vai precisar disto para abrir a ficha de uma compra passada
    @GetMapping("/{id}")
    public ResponseEntity<CompraResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<Page<CompraResponseDTO>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.listarMinhasCompras(page, size));
    }

    // 🛡️ ADICIONADO: O padrão perfeito para eliminações (Status 204)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // 🛡️ CORREÇÃO: Agora respeita o contrato do ResponseEntity
    @GetMapping("/taxas-iva")
    public ResponseEntity<List<TxIva>> listarTaxasIva() {
        // 💡 Dica Industrial: O ideal a longo prazo é criares um TxIvaResponseDTO
        // e mapeares aqui, para garantir que 100% dos controllers só cospem DTOs!
        return ResponseEntity.ok(service.listarTaxasIva());
    }
}