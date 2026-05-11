package pt.gestorflow.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.MovimentoPlaneadoDTO;
import pt.gestorflow.backend.service.PlaneamentoService;

import java.util.List;

@RestController
@RequestMapping("/api/planeamento")
@RequiredArgsConstructor
public class PlaneamentoController {

    private final PlaneamentoService planeamentoService;

    @PostMapping
    public ResponseEntity<MovimentoPlaneadoDTO> criarPlano(@RequestBody MovimentoPlaneadoDTO dto) {
        return ResponseEntity.ok(planeamentoService.criarPlano(dto));
    }

    @GetMapping
    public ResponseEntity<List<MovimentoPlaneadoDTO>> listarPlanos() {
        return ResponseEntity.ok(planeamentoService.listarPlanos());
    }

    @PutMapping("/{id}")
    public ResponseEntity<MovimentoPlaneadoDTO> atualizarPlano(@PathVariable Long id, @RequestBody MovimentoPlaneadoDTO dto) {
        return ResponseEntity.ok(planeamentoService.atualizarPlano(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagarPlano(@PathVariable Long id) {
        planeamentoService.apagarPlano(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Void> alternarStatus(@PathVariable Long id) {
        planeamentoService.alternarStatus(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/gerar-fatura")
    public ResponseEntity<Void> gerarFaturaPendente(@PathVariable Long id) {
        planeamentoService.gerarFaturaPendente(id);
        return ResponseEntity.ok().build();
    }
}