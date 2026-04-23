package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.*;
import pt.gestorflow.backend.service.PatrimonioService;

@RestController
@RequestMapping("/api/patrimonio")
@RequiredArgsConstructor
public class PatrimonioController {

    private final PatrimonioService service;

    // 🛡️ CONTRATO UNIFICADO: Devolve sempre ResponseEntity
    @GetMapping
    public ResponseEntity<Page<PatrimonioResponseDTO>> listar(Pageable pageable) {
        return ResponseEntity.ok(service.listarPatrimonio(pageable));
    }

    // 🛡️ ADICIONADO: Essencial para o Angular abrir o detalhe de um ativo
    @GetMapping("/{id}")
    public ResponseEntity<PatrimonioResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // 🛡️ 201 CREATED: O standard para novos registos
    @PostMapping("/viaturas")
    public ResponseEntity<PatrimonioResponseDTO> criarViatura(@Valid @RequestBody PatrimonioViaturaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarViatura(dto));
    }

    @PostMapping("/imoveis")
    public ResponseEntity<PatrimonioResponseDTO> criarImovel(@Valid @RequestBody PatrimonioImovelDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarImovel(dto));
    }

    @PostMapping("/ferramentas")
    public ResponseEntity<PatrimonioResponseDTO> criarFerramenta(@Valid @RequestBody PatrimonioFerramentaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarFerramenta(dto));
    }

    // 🛡️ 204 NO CONTENT: Perfeito.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}