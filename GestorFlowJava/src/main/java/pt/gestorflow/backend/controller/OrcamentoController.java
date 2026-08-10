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
import pt.gestorflow.backend.dto.OrcamentoDTO;
import pt.gestorflow.backend.dto.OrcamentoResponseDTO;
import pt.gestorflow.backend.model.Orcamento; // O uso do Enum da entidade aqui é seguro e perfeitamente aceitável!
import pt.gestorflow.backend.service.OrcamentoService;

@Slf4j // 🚀 Telemetria de entrada
@RestController
@RequestMapping("/api/orcamentos")
@RequiredArgsConstructor
@Tag(name = "Orçamentos", description = "Gestão de orçamentos e conversão para vendas faturadas")
public class OrcamentoController {

    private final OrcamentoService service;

    // 🛡️ 201 CREATED: O standard para o momento em que um orçamento nasce
    @Operation(summary = "Criar novo Orçamento", description = "Gera uma nova proposta comercial (orçamento) para um cliente.")
    @PostMapping
    public ResponseEntity<OrcamentoResponseDTO> criar(@Valid @RequestBody OrcamentoDTO dto) {
        log.debug("Pedido HTTP POST recebido: /api/orcamentos");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarOrcamento(dto));
    }

    @Operation(summary = "Atualizar Orçamento", description = "Edita os dados, linhas ou datas de um orçamento existente.")
    @PutMapping("/{id}")
    public ResponseEntity<OrcamentoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody OrcamentoDTO dto) {
        log.debug("Pedido HTTP PUT recebido: /api/orcamentos/{}", id);
        return ResponseEntity.ok(service.atualizarOrcamento(id, dto));
    }

    @Operation(summary = "Listar Orçamentos", description = "Devolve o histórico de orçamentos do utilizador, com paginação.")
    @GetMapping
    public ResponseEntity<Page<OrcamentoResponseDTO>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        log.debug("Pedido HTTP GET recebido: /api/orcamentos (Página: {}, Tamanho: {})", pagina, tamanho);
        return ResponseEntity.ok(service.listarMeusOrcamentos(pagina, tamanho));
    }

    @Operation(summary = "Obter detalhe do Orçamento", description = "Devolve a informação completa de um orçamento específico pelo seu ID.")
    @GetMapping("/{id}")
    public ResponseEntity<OrcamentoResponseDTO> buscarPorId(@PathVariable Long id) {
        log.debug("Pedido HTTP GET recebido: /api/orcamentos/{}", id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // 🛡️ 204 NO CONTENT: Certinho.
    @Operation(summary = "Eliminar Orçamento", description = "Apaga permanentemente um orçamento do sistema.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.debug("Pedido HTTP DELETE recebido: /api/orcamentos/{}", id);
        service.eliminarOrcamento(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Alterar Estado do Orçamento", description = "Atualiza apenas o status do orçamento (ex: PENDENTE, APROVADO, REJEITADO).")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<OrcamentoResponseDTO> alterarEstado(
            @PathVariable Long id,
            @RequestParam Orcamento.EstadoOrcamento estado) {

        log.info("Auditoria Comercial: Pedido para alterar o estado do Orçamento ID: {} para {}", id, estado);
        return ResponseEntity.ok(service.alterarEstado(id, estado));
    }

    // 🛡️ Mantém-se 200 OK. Como é uma ação /RPC style (Remote Procedure Call), o 200 é perfeito.
    @Operation(summary = "Converter Orçamento em Venda", description = "Transforma um orçamento aprovado numa venda definitiva e gera os devidos impactos no sistema (stock/tesouraria).")
    @PostMapping("/{id}/converter-venda")
    public ResponseEntity<Void> converterEmVenda(
            @PathVariable Long id,
            @RequestParam Long contaBancariaId) {

        log.info("Auditoria Comercial: Pedido de conversão do Orçamento ID: {} numa Venda Efetiva. (Conta Bancária Destino ID: {})", id, contaBancariaId);
        service.converterEmVenda(id, contaBancariaId);

        return ResponseEntity.ok().build();
    }
}