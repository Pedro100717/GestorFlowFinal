package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.ArtigoDTO;
import pt.gestorflow.backend.dto.ArtigoResponseDTO;
import pt.gestorflow.backend.service.ArtigoService;

@RestController
@RequestMapping("/api/artigos")
@RequiredArgsConstructor
public class ArtigoController {

    private final ArtigoService service;

    // 🛡️ 201 CREATED: O padrão correto da indústria para novos registos
    @PostMapping
    public ResponseEntity<ArtigoResponseDTO> criar(@Valid @RequestBody ArtigoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarArtigo(dto));
    }

    // 🛡️ ADICIONADO: O Angular vai precisar disto para preencher o ecrã de edição!
    @GetMapping("/{id}")
    public ResponseEntity<ArtigoResponseDTO> buscarPorId(@PathVariable Long id) {
        // (Garante que tens este método no teu ArtigoService)
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<Page<ArtigoResponseDTO>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.listarMeusArtigos(page, size));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArtigoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody ArtigoDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    // 🛡️ 204 NO CONTENT: Está perfeito, é exatamente o que um DELETE deve devolver.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}