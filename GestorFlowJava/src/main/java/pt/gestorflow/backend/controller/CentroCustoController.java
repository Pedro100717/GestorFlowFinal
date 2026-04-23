package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.CentroCustoDTO;
import pt.gestorflow.backend.dto.CentroCustoResponseDTO;
import pt.gestorflow.backend.service.CentroCustoService;

import java.util.List;

@RestController
@RequestMapping("/api/centros-custo")
@RequiredArgsConstructor
public class CentroCustoController {

    private final CentroCustoService service;

    // 🛡️ 201 CREATED: Padronizado para criação com sucesso
    @PostMapping
    public ResponseEntity<CentroCustoResponseDTO> criar(@Valid @RequestBody CentroCustoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    // 🛡️ ADICIONADO: Endpoint obrigatório para o Angular conseguir abrir a página de edição
    @GetMapping("/{id}")
    public ResponseEntity<CentroCustoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // 🛡️ CONTRATO UNIFICADO: Passou de "List<...>" direto para "ResponseEntity<List<...>>"
    @GetMapping
    public ResponseEntity<List<CentroCustoResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CentroCustoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody CentroCustoDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    // 🛡️ ADICIONADO: Faltava o método de apagar. Status 204 (No Content) é o padrão.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}