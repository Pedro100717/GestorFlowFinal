package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.FornecedorDTO;
import pt.gestorflow.backend.dto.FornecedorResponseDTO;
import pt.gestorflow.backend.service.FornecedorService;

import java.util.List;

@RestController
@RequestMapping("/api/fornecedores")
@RequiredArgsConstructor
public class FornecedorController {

    private final FornecedorService service;

    // 🛡️ CONTRATO UNIFICADO: Passou a ResponseEntity<List<...>>
    @GetMapping
    public ResponseEntity<List<FornecedorResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    // 🛡️ ADICIONADO: O Angular precisa disto para carregar a ficha do Fornecedor na edição
    @GetMapping("/{id}")
    public ResponseEntity<FornecedorResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // 🛡️ 201 CREATED: O standard REST
    @PostMapping
    public ResponseEntity<FornecedorResponseDTO> criar(@Valid @RequestBody FornecedorDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FornecedorResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody FornecedorDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    // 🛡️ 204 NO CONTENT: Perfeito.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}