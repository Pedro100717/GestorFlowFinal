package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.SeccaoHomoDTO;
import pt.gestorflow.backend.dto.SeccaoHomoResponseDTO;
import pt.gestorflow.backend.model.SeccaoHomo;
import pt.gestorflow.backend.service.SeccaoHomoService;
import java.util.List;

@RestController
@RequestMapping("/api/seccoes-homogeneas")
@RequiredArgsConstructor
public class SeccaoHomoController {

    private final SeccaoHomoService service;

    @PostMapping
    public ResponseEntity<SeccaoHomoResponseDTO> criar(@Valid @RequestBody SeccaoHomoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeccaoHomoResponseDTO> buscaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<SeccaoHomoResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<SeccaoHomoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody SeccaoHomoDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}