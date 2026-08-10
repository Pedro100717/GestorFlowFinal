package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // 🚀 Documentação OpenAPI
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.ArtigoDTO;
import pt.gestorflow.backend.dto.ArtigoResponseDTO;
import pt.gestorflow.backend.service.ArtigoService;

@Slf4j // 🚀 Telemetria de entrada
@RestController
@RequestMapping("/api/artigos")
@RequiredArgsConstructor
@Tag(name = "Artigos (Produtos e Serviços)", description = "Gestão do catálogo de inventário e serviços da empresa")
public class ArtigoController {

    private final ArtigoService service;

    @Operation(summary = "Criar novo artigo", description = "Regista um novo produto (mercadoria) ou serviço no sistema.")
    @PostMapping
    public ResponseEntity<ArtigoResponseDTO> criar(@Valid @RequestBody ArtigoDTO dto) {
        log.debug("Pedido HTTP POST recebido: /api/artigos");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarArtigo(dto));
    }

    @Operation(summary = "Obter detalhe do artigo", description = "Devolve os dados completos de um artigo específico pelo seu ID.")
    @GetMapping("/{id}")
    public ResponseEntity<ArtigoResponseDTO> buscarPorId(@PathVariable Long id) {
        log.debug("Pedido HTTP GET recebido: /api/artigos/{}", id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @Operation(summary = "Listar artigos paginados", description = "Devolve a lista do catálogo do utilizador autenticado, com paginação.")
    @GetMapping
    public ResponseEntity<Page<ArtigoResponseDTO>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.debug("Pedido HTTP GET recebido: /api/artigos (Página: {}, Tamanho: {})", page, size);
        return ResponseEntity.ok(service.listarMeusArtigos(page, size));
    }

    @Operation(summary = "Atualizar artigo", description = "Edita os dados de um produto ou serviço existente.")
    @PutMapping("/{id}")
    public ResponseEntity<ArtigoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody ArtigoDTO dto) {
        log.debug("Pedido HTTP PUT recebido: /api/artigos/{}", id);
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @Operation(summary = "Eliminar artigo", description = "Apaga ou desativa um artigo do catálogo (Soft Delete dependendo da lógica do serviço).")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.debug("Pedido HTTP DELETE recebido: /api/artigos/{}", id);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}