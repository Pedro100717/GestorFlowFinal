package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // 🚀 Documentação OpenAPI
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.FornecedorDTO;
import pt.gestorflow.backend.dto.FornecedorResponseDTO;
import pt.gestorflow.backend.service.FornecedorService;

import java.util.List;

@Slf4j // 🚀 Telemetria de entrada
@RestController
@RequestMapping("/api/fornecedores")
@RequiredArgsConstructor
@Tag(name = "Fornecedores", description = "Gestão de fornecedores e parceiros de negócio")
public class FornecedorController {

    private final FornecedorService service;

    // 🛡️ CONTRATO UNIFICADO: Passou a ResponseEntity<List<...>>
    @Operation(summary = "Listar Fornecedores", description = "Devolve a lista completa de fornecedores associados à conta do utilizador.")
    @GetMapping
    public ResponseEntity<List<FornecedorResponseDTO>> listar() {
        log.debug("Pedido HTTP GET recebido: /api/fornecedores");
        return ResponseEntity.ok(service.listar());
    }

    // 🛡️ ADICIONADO: O Angular precisa disto para carregar a ficha do Fornecedor na edição
    @Operation(summary = "Obter detalhe do Fornecedor", description = "Devolve os dados completos de um fornecedor específico através do seu ID.")
    @GetMapping("/{id}")
    public ResponseEntity<FornecedorResponseDTO> buscarPorId(@PathVariable Long id) {
        log.debug("Pedido HTTP GET recebido: /api/fornecedores/{}", id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // 🛡️ 201 CREATED: O standard REST
    @Operation(summary = "Criar novo Fornecedor", description = "Regista um novo fornecedor no sistema.")
    @PostMapping
    public ResponseEntity<FornecedorResponseDTO> criar(@Valid @RequestBody FornecedorDTO dto) {
        log.debug("Pedido HTTP POST recebido: /api/fornecedores");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    @Operation(summary = "Atualizar Fornecedor", description = "Altera os dados cadastrais de um fornecedor existente.")
    @PutMapping("/{id}")
    public ResponseEntity<FornecedorResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody FornecedorDTO dto) {
        log.debug("Pedido HTTP PUT recebido: /api/fornecedores/{}", id);
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    // 🛡️ 204 NO CONTENT: Perfeito.
    @Operation(summary = "Eliminar Fornecedor", description = "Remove um fornecedor do sistema com base no seu ID.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.debug("Pedido HTTP DELETE recebido: /api/fornecedores/{}", id);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}