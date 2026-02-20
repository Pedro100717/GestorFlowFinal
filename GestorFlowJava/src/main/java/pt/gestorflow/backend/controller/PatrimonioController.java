package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.*;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.service.PatrimonioService;

import java.util.List;

@RestController
@RequestMapping("/api/patrimonio")
@RequiredArgsConstructor
public class PatrimonioController {

    private final PatrimonioService service;

    // Listar tudo
    @GetMapping
    public List<Patrimonio> listar() {
        return service.listarPatrimonio();
    }

    // Criar Viatura
    @PostMapping("/viaturas")
    public ResponseEntity<PatrimonioViatura> criarViatura(@Valid @RequestBody PatrimonioViaturaDTO dto) {
        return ResponseEntity.ok(service.criarViatura(dto));
    }

    // Criar Imovel
    @PostMapping("/imoveis")
    public ResponseEntity<PatrimonioImovel> criarImovel(@Valid @RequestBody PatrimonioImovelDTO dto) {
        return ResponseEntity.ok(service.criarImovel(dto));
    }

    // Criar Ferramenta
    @PostMapping("/ferramentas")
    public ResponseEntity<PatrimonioFerramenta> criarFerramenta(@Valid @RequestBody PatrimonioFerramentaDTO dto) {
        return ResponseEntity.ok(service.criarFerramenta(dto));
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}