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
import pt.gestorflow.backend.dto.MovimentoStockDTO;
import pt.gestorflow.backend.dto.MovimentoStockResponseDTO;
import pt.gestorflow.backend.service.MovimentoStockService;

@Slf4j // 🚀 Telemetria ativada
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Tag(name = "Movimentos de Stock", description = "Gestão de inventário, histórico de entradas/saídas e acertos manuais")
public class MovimentoStockController {

    private final MovimentoStockService service;

    // 🛡️ 201 CREATED: O padrão correto para novos registos na BD
    @Operation(summary = "Registar Acerto Manual", description = "Cria um movimento de stock manual (entrada ou saída) para correção de inventário.")
    @PostMapping("/acerto")
    public ResponseEntity<MovimentoStockResponseDTO> registarAcerto(
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey, // 🚀 A CHAVE ENTRA AQUI
            @Valid @RequestBody MovimentoStockDTO dto) {

        log.info("Auditoria de Armazém: Pedido HTTP POST recebido para Acerto de Stock. Mercadoria ID: {}, Tipo: {}, Quantidade: {} (Chave: {})",
                dto.getMercadoriaId(), dto.getTipo(), dto.getQuantidade(), idempotencyKey);

        // Passamos a chave para o serviço!
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registarAcerto(dto, idempotencyKey));
    }

    // 🛡️ ADICIONADO: Essencial para o Angular abrir o detalhe de um movimento específico
    @Operation(summary = "Obter detalhe do Movimento", description = "Devolve a informação completa de um movimento de stock específico pelo seu ID.")
    @GetMapping("/historico/{id}")
    public ResponseEntity<MovimentoStockResponseDTO> buscarPorId(@PathVariable Long id) {
        log.debug("Pedido HTTP GET recebido: /api/stock/historico/{}", id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @Operation(summary = "Histórico Global de Stock", description = "Devolve o histórico de todos os movimentos de stock da empresa com paginação.")
    @GetMapping("/historico")
    public ResponseEntity<Page<MovimentoStockResponseDTO>> listarHistorico(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.debug("Pedido HTTP GET recebido: /api/stock/historico (Página: {}, Tamanho: {})", page, size);
        return ResponseEntity.ok(service.listarHistorico(page, size));
    }

    @Operation(summary = "Histórico de Stock por Artigo", description = "Devolve o histórico de movimentos filtrado apenas para um artigo específico.")
    @GetMapping("/artigo/{artigoId}")
    public ResponseEntity<Page<MovimentoStockResponseDTO>> listarHistoricoDoArtigo(
            @PathVariable Long artigoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.debug("Pedido HTTP GET recebido: /api/stock/artigo/{} (Página: {}, Tamanho: {})", artigoId, page, size);

        // (Nota: Terás de criar o método listarPorArtigo no teu MovimentoStockService)
        return ResponseEntity.ok(service.listarPorArtigo(artigoId, page, size));
    }
}