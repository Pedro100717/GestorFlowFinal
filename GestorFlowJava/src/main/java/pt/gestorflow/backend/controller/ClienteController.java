package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // 🚀 Documentação OpenAPI
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.ClienteDTO;
import pt.gestorflow.backend.dto.ClienteResponseDTO;
import pt.gestorflow.backend.service.ClienteService;

import java.util.List;

@Slf4j // 🚀 Telemetria de entrada
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gestão de clientes e respetivos dados de faturação e contacto")
public class ClienteController {

    private final ClienteService service;

    // 🛡️ 201 CREATED: O standard REST para quando um recurso nasce na base de dados
    @Operation(summary = "Criar novo cliente", description = "Regista um novo cliente associado ao utilizador autenticado.")
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> criar(@Valid @RequestBody ClienteDTO dto) {
        log.debug("Pedido HTTP POST recebido: /api/clientes");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarCliente(dto));
    }

    // 🛡️ ADICIONADO: Obrigatório para o Angular preencher o formulário de edição do Cliente
    @Operation(summary = "Obter detalhe do cliente", description = "Devolve os dados completos de um cliente específico através do seu ID.")
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable Long id) {
        log.debug("Pedido HTTP GET recebido: /api/clientes/{}", id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @Operation(summary = "Listar clientes paginados", description = "Devolve a lista paginada dos clientes da conta do utilizador.")
    @GetMapping
    public ResponseEntity<Page<ClienteResponseDTO>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.debug("Pedido HTTP GET recebido: /api/clientes (Página: {}, Tamanho: {})", page, size);
        return ResponseEntity.ok(service.listarMeusClientes(page, size));
    }

    @Operation(summary = "Atualizar cliente", description = "Altera os dados cadastrais de um cliente existente.")
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody ClienteDTO dto) {
        log.debug("Pedido HTTP PUT recebido: /api/clientes/{}", id);
        return ResponseEntity.ok(service.atualizarCliente(id, dto));
    }

    // 🛡️ 204 NO CONTENT: O padrão perfeito para eliminações
    @Operation(summary = "Eliminar cliente", description = "Remove um cliente do sistema com base no seu ID.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.debug("Pedido HTTP DELETE recebido: /api/clientes/{}", id);
        service.eliminarCliente(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Importar clientes em lote", description = "Recebe uma lista de clientes validados e grava-os de forma transacional (Tudo ou Nada).")
    @PostMapping("/importar")
    public ResponseEntity<List<ClienteResponseDTO>> importarEmLote(@Valid @RequestBody List<ClienteDTO> dtos) {
        log.debug("Pedido HTTP POST recebido: /api/clientes/importar ({} registos)", dtos.size());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.importarEmLote(dtos));
    }
}