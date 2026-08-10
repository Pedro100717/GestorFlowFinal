package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // 🚀 Documentação OpenAPI
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.CentroCustoDTO;
import pt.gestorflow.backend.dto.CentroCustoResponseDTO;
import pt.gestorflow.backend.service.CentroCustoService;

import java.util.List;

@Slf4j // 🚀 Telemetria de entrada
@RestController
@RequestMapping("/api/centros-custo")
@RequiredArgsConstructor
@Tag(name = "Centros de Custo", description = "Gestão da estrutura analítica de centros de custo da empresa")
public class CentroCustoController {

    private final CentroCustoService service;

    // 🛡️ 201 CREATED: Padronizado para criação com sucesso
    @Operation(summary = "Criar Centro de Custo", description = "Regista um novo centro de custo associado ao utilizador autenticado.")
    @PostMapping
    public ResponseEntity<CentroCustoResponseDTO> criar(@Valid @RequestBody CentroCustoDTO dto) {
        log.debug("Pedido HTTP POST recebido: /api/centros-custo");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    // 🛡️ ADICIONADO: Endpoint obrigatório para o Angular conseguir abrir a página de edição
    @Operation(summary = "Obter detalhe do Centro de Custo", description = "Devolve os dados de um centro de custo específico.")
    @GetMapping("/{id}")
    public ResponseEntity<CentroCustoResponseDTO> buscarPorId(@PathVariable Long id) {
        log.debug("Pedido HTTP GET recebido: /api/centros-custo/{}", id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // 🛡️ CONTRATO UNIFICADO: Passou de "List<...>" direto para "ResponseEntity<List<...>>"
    @Operation(summary = "Listar Centros de Custo", description = "Devolve a lista completa de centros de custo ativos do utilizador.")
    @GetMapping
    public ResponseEntity<List<CentroCustoResponseDTO>> listar() {
        log.debug("Pedido HTTP GET recebido: /api/centros-custo");
        return ResponseEntity.ok(service.listar());
    }

    @Operation(summary = "Atualizar Centro de Custo", description = "Edita os dados (nome, código) de um centro de custo existente.")
    @PutMapping("/{id}")
    public ResponseEntity<CentroCustoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody CentroCustoDTO dto) {
        log.debug("Pedido HTTP PUT recebido: /api/centros-custo/{}", id);
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    // 🛡️ ADICIONADO: Faltava o método de apagar. Status 204 (No Content) é o padrão.
    @Operation(summary = "Eliminar Centro de Custo", description = "Apaga permanentemente um centro de custo do sistema.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.debug("Pedido HTTP DELETE recebido: /api/centros-custo/{}", id);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}