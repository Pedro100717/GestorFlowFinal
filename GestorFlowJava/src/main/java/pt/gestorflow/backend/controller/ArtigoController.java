package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.ArtigoDTO;
import pt.gestorflow.backend.model.Artigo;
import pt.gestorflow.backend.service.ArtigoService;

@RestController
@RequestMapping("/api/artigos")
@RequiredArgsConstructor
public class ArtigoController {

    private final ArtigoService service;

    @PostMapping
    public ResponseEntity<Artigo> criar(@Valid @RequestBody ArtigoDTO dto) {
        return ResponseEntity.ok(service.criarArtigo(dto));
    }

    @GetMapping
    public ResponseEntity<Page<Artigo>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.listarMeusArtigos(page, size));
    }

    // --- REMOVIDO: listarTaxasIva daqui ---
    // O artigo já não "conhece" taxas.

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Artigo> atualizar(@PathVariable Long id, @Valid @RequestBody ArtigoDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }
}