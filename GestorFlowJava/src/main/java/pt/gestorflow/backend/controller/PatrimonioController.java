package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.*;
import pt.gestorflow.backend.service.PatrimonioService;

import java.util.List;

@RestController
@RequestMapping("/api/patrimonio")
@RequiredArgsConstructor
public class PatrimonioController {

    private final PatrimonioService service;

    @GetMapping
    public Page<PatrimonioResponseDTO> listar(Pageable pageable) {
        return service.listarPatrimonio(pageable);
    }

    @PostMapping("/viaturas")
    public ResponseEntity<PatrimonioResponseDTO> criarViatura(@Valid @RequestBody PatrimonioViaturaDTO dto) {
        return ResponseEntity.ok(service.criarViatura(dto));
    }

    @PostMapping("/imoveis")
    public ResponseEntity<PatrimonioResponseDTO> criarImovel(@Valid @RequestBody PatrimonioImovelDTO dto) {
        return ResponseEntity.ok(service.criarImovel(dto));
    }

    @PostMapping("/ferramentas")
    public ResponseEntity<PatrimonioResponseDTO> criarFerramenta(@Valid @RequestBody PatrimonioFerramentaDTO dto) {
        return ResponseEntity.ok(service.criarFerramenta(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}