package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public List<SeccaoHomoResponseDTO> listar() { return service.listar(); }

    @PostMapping
    public ResponseEntity<SeccaoHomoResponseDTO> criar(@Valid @RequestBody SeccaoHomoDTO dto) {
        return ResponseEntity.ok(service.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SeccaoHomoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody SeccaoHomoDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }
}