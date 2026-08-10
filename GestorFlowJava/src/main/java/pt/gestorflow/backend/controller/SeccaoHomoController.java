package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // 🚀 Documentação OpenAPI
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.SeccaoHomoDTO;
import pt.gestorflow.backend.dto.SeccaoHomoResponseDTO;
import pt.gestorflow.backend.service.SeccaoHomoService;

import java.util.List;

@Slf4j // 🚀 Telemetria de entrada
@RestController
@RequestMapping("/api/seccoes-homogeneas")
@RequiredArgsConstructor
@Tag(name = "Secções Homogéneas", description = "Gestão de secções produtivas/departamentos para controlo de gestão")
public class SeccaoHomoController {

    private final SeccaoHomoService service;

    @Operation(summary = "Criar Secção Homogénea", description = "Regista uma nova secção associada à conta do utilizador.")
    @PostMapping
    public ResponseEntity<SeccaoHomoResponseDTO> criar(@Valid @RequestBody SeccaoHomoDTO dto) {
        log.debug("Pedido HTTP POST recebido: /api/seccoes-homogeneas");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    @Operation(summary = "Obter detalhe da Secção", description = "Devolve os dados completos de uma secção através do seu ID.")
    @GetMapping("/{id}")
    public ResponseEntity<SeccaoHomoResponseDTO> buscaPorId(@PathVariable Long id) {
        log.debug("Pedido HTTP GET recebido: /api/seccoes-homogeneas/{}", id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @Operation(summary = "Listar Secções", description = "Devolve a lista completa de secções homogéneas ativas do utilizador.")
    @GetMapping
    public ResponseEntity<List<SeccaoHomoResponseDTO>> listar() {
        log.debug("Pedido HTTP GET recebido: /api/seccoes-homogeneas");
        return ResponseEntity.ok(service.listar());
    }

    @Operation(summary = "Atualizar Secção", description = "Altera os dados (código, nome, etc.) de uma secção existente.")
    @PutMapping("/{id}")
    public ResponseEntity<SeccaoHomoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody SeccaoHomoDTO dto) {
        log.debug("Pedido HTTP PUT recebido: /api/seccoes-homogeneas/{}", id);
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @Operation(summary = "Eliminar Secção", description = "Remove uma secção do sistema.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.debug("Pedido HTTP DELETE recebido: /api/seccoes-homogeneas/{}", id);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}