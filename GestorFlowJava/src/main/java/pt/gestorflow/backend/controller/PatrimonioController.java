package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // 🚀 Documentação OpenAPI
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.*;
import pt.gestorflow.backend.service.PatrimonioService;

@Slf4j // 🚀 Telemetria ativada
@RestController
@RequestMapping("/api/patrimonio")
@RequiredArgsConstructor
@Tag(name = "Património", description = "Gestão de ativos imobilizados (Viaturas, Imóveis, Ferramentas)")
public class PatrimonioController {

    private final PatrimonioService service;

    // 🛡️ CONTRATO UNIFICADO: Devolve sempre ResponseEntity
    @Operation(summary = "Listar Património", description = "Devolve a lista paginada de todos os ativos da empresa.")
    @GetMapping
    public ResponseEntity<Page<PatrimonioResponseDTO>> listar(Pageable pageable) {
        log.debug("Pedido HTTP GET recebido: /api/patrimonio (Pageable: {})", pageable);
        return ResponseEntity.ok(service.listarPatrimonio(pageable));
    }

    // 🛡️ ADICIONADO: Essencial para o Angular abrir o detalhe de um ativo
    @Operation(summary = "Obter detalhe do Ativo", description = "Devolve a informação completa de um bem patrimonial específico.")
    @GetMapping("/{id}")
    public ResponseEntity<PatrimonioResponseDTO> buscarPorId(@PathVariable Long id) {
        log.debug("Pedido HTTP GET recebido: /api/patrimonio/{}", id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // 🛡️ 201 CREATED: O standard para novos registos
    @Operation(summary = "Registar Viatura", description = "Adiciona um novo veículo à frota da empresa.")
    @PostMapping("/viaturas")
    public ResponseEntity<PatrimonioResponseDTO> criarViatura(@Valid @RequestBody PatrimonioViaturaDTO dto) {
        log.info("Auditoria de Património: Registo de nova viatura solicitado (Matrícula: {})", dto.getMatricula());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarViatura(dto));
    }

    @Operation(summary = "Registar Imóvel", description = "Adiciona um novo imóvel ou infraestrutura aos ativos da empresa.")
    @PostMapping("/imoveis")
    public ResponseEntity<PatrimonioResponseDTO> criarImovel(@Valid @RequestBody PatrimonioImovelDTO dto) {
        log.info("Auditoria de Património: Registo de novo imóvel solicitado.");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarImovel(dto));
    }

    @Operation(summary = "Registar Ferramenta/Equipamento", description = "Adiciona uma nova ferramenta ou equipamento ao património.")
    @PostMapping("/ferramentas")
    public ResponseEntity<PatrimonioResponseDTO> criarFerramenta(@Valid @RequestBody PatrimonioFerramentaDTO dto) {
        log.info("Auditoria de Património: Registo de nova ferramenta/equipamento solicitado.");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarFerramenta(dto));
    }

    // 🛡️ 204 NO CONTENT: Perfeito.
    @Operation(summary = "Eliminar Ativo", description = "Remove (ou abate) um ativo do património da empresa.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("Auditoria de Património: Pedido de eliminação/abate do ativo ID: {}", id);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}