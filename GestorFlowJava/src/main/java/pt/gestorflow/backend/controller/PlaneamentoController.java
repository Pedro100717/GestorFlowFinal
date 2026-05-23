package pt.gestorflow.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.MovimentoPlaneadoDTO;
import pt.gestorflow.backend.service.PlaneamentoService;

import java.time.LocalDate;
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

    // =========================================================================
    // --- MÁQUINA DO TEMPO: EXCEÇÕES (ESTILO GOOGLE CALENDAR) 🚀 ---
    // =========================================================================

    // 🚀 1. APAGAR APENAS UM MÊS (EXCEÇÃO)
    @DeleteMapping("/{id}/excecao")
    public ResponseEntity<Void> ignorarDataDoPlano(
            @PathVariable Long id,
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAignorar) {

        planeamentoService.ignorarDataPlano(id, dataAignorar);
        return ResponseEntity.noContent().build();
    }

    // 🚀 2. EDITAR APENAS UM MÊS (EXCEÇÃO - SUBSTITUIÇÃO)
    @PostMapping("/{id}/excecao")
    public ResponseEntity<MovimentoPlaneadoDTO> criarExcecao(
            @PathVariable Long id,
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataOriginal,
            @RequestBody MovimentoPlaneadoDTO dtoNovo) {

        MovimentoPlaneadoDTO excecaoCriada = planeamentoService.criarExcecaoPlano(id, dataOriginal, dtoNovo);
        return ResponseEntity.ok(excecaoCriada);
    }
}