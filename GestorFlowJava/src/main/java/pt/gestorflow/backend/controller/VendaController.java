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
import pt.gestorflow.backend.dto.VendaDTO;
import pt.gestorflow.backend.dto.VendaResponseDTO;
import pt.gestorflow.backend.service.VendaService;

// 🚀 IMPORT REMOVIDO: pt.gestorflow.backend.model.TxIva; (Entidade não entra no Controller!)

@Slf4j // 🚀 Telemetria de alta segurança ativada
@RestController
@RequestMapping("/api/vendas")
@RequiredArgsConstructor
@Tag(name = "Vendas e Faturação", description = "Registo de vendas, faturação e impacto direto na tesouraria e stock")
public class VendaController {

    private final VendaService service;

    // 🛡️ 201 CREATED: O Standard
    @Operation(summary = "Registar Venda", description = "Cria uma nova venda, abata o stock correspondente e gera os registos de tesouraria.")
    @PostMapping
    public ResponseEntity<VendaResponseDTO> criar(@Valid @RequestBody VendaDTO dto) {
        log.info("Auditoria Comercial: Nova venda registada com sucesso pelo utilizador.");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registarVenda(dto));
    }

    // 🚀 ENDPOINT REMOVIDO: /taxas-iva foi apagado porque já existe no TxIvaController!

    @Operation(summary = "Obter detalhe da Venda", description = "Devolve a informação completa e as linhas de uma venda específica pelo seu ID.")
    @GetMapping("/{id}")
    public ResponseEntity<VendaResponseDTO> buscarPorId(@PathVariable Long id) {
        log.debug("Pedido HTTP GET recebido: /api/vendas/{}", id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @Operation(summary = "Listar Vendas", description = "Devolve o histórico de vendas da empresa com suporte a paginação.")
    @GetMapping
    public ResponseEntity<Page<VendaResponseDTO>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Pedido HTTP GET recebido: /api/vendas (Página: {}, Tamanho: {})", page, size);
        return ResponseEntity.ok(service.listarMinhasVendas(page, size));
    }

    // ==========================================
    // --- 🚀 A PORTA PARA A EDIÇÃO (UPDATE) ---
    // ==========================================
    @Operation(summary = "Atualizar Venda", description = "Edita os dados ou as linhas de uma venda existente (se o estado permitir).")
    @PutMapping("/{id}")
    public ResponseEntity<VendaResponseDTO> atualizarVenda(@PathVariable Long id, @Valid @RequestBody VendaDTO dto) {
        log.info("Auditoria Comercial: Pedido de alteração na Venda ID: {}", id);
        return ResponseEntity.ok(service.atualizarVenda(id, dto));
    }

    @Operation(summary = "Anular Venda", description = "Reverte a venda, cancelando faturas pendentes e devolvendo o artigo ao stock.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anular(@PathVariable Long id) {
        log.warn("Auditoria Comercial - ALERTA: Pedido de anulação para a Venda ID: {}. Stock e tesouraria serão revertidos.", id);
        service.anularVenda(id);
        return ResponseEntity.noContent().build();
    }
}