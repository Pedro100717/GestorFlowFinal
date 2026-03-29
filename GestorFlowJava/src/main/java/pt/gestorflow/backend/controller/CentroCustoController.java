package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.CentroCustoDTO;
import pt.gestorflow.backend.dto.CentroCustoResponseDTO;
import pt.gestorflow.backend.model.CentroCusto;
import pt.gestorflow.backend.service.CentroCustoService;
import java.util.List;

@RestController
@RequestMapping("/api/centros-custo")
@RequiredArgsConstructor
public class CentroCustoController {

    private final CentroCustoService service;

    @GetMapping
    public List<CentroCustoResponseDTO> listar() { return service.listar(); }

    @PostMapping
    public ResponseEntity<CentroCustoResponseDTO> criar(@Valid @RequestBody CentroCustoDTO dto) {
        return ResponseEntity.ok(service.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CentroCustoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody CentroCustoDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }
}