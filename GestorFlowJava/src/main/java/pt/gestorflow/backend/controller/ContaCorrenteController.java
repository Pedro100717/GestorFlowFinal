package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // 🚀 Documentação OpenAPI
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.ContaCorrenteExtratoDTO;
import pt.gestorflow.backend.dto.ContaCorrenteResumoDTO;
import pt.gestorflow.backend.service.ContaCorrenteService;

import java.util.List;

@Slf4j // 🚀 Telemetria de entrada
@RestController
@RequestMapping("/api/contas-correntes")
@RequiredArgsConstructor
@Tag(name = "Contas Correntes", description = "Consultas financeiras e extratos de Clientes e Fornecedores")
public class ContaCorrenteController {

    private final ContaCorrenteService service;

    @Operation(summary = "Resumo de Clientes", description = "Devolve a lista agregada dos saldos em dívida e valores faturados por cliente.")
    @GetMapping("/clientes/resumo")
    public ResponseEntity<List<ContaCorrenteResumoDTO>> getResumoClientes() {
        log.debug("Pedido HTTP GET recebido: /api/contas-correntes/clientes/resumo");
        // 🚀 Agora devolve o DTO traduzido com as chaves 'nome' e 'totalFaturado'
        return ResponseEntity.ok(service.obterResumoClientes());
    }

    @Operation(summary = "Extrato de Cliente", description = "Devolve o histórico detalhado de faturas e recibos de um cliente específico.")
    @GetMapping("/clientes/{id}/extrato")
    public ResponseEntity<List<ContaCorrenteExtratoDTO>> getExtratoCliente(@PathVariable Long id) {
        log.debug("Pedido HTTP GET recebido: /api/contas-correntes/clientes/{}/extrato", id);
        return ResponseEntity.ok(service.obterExtratoCliente(id));
    }

    @Operation(summary = "Resumo de Fornecedores", description = "Devolve a lista agregada dos saldos a pagar e valores comprados por fornecedor.")
    @GetMapping("/fornecedores/resumo")
    public ResponseEntity<List<ContaCorrenteResumoDTO>> getResumoFornecedores() {
        log.debug("Pedido HTTP GET recebido: /api/contas-correntes/fornecedores/resumo");
        // 🚀 O mesmo aqui para os Fornecedores!
        return ResponseEntity.ok(service.obterResumoFornecedores());
    }

    @Operation(summary = "Extrato de Fornecedor", description = "Devolve o histórico detalhado de faturas e pagamentos de um fornecedor específico.")
    @GetMapping("/fornecedores/{id}/extrato")
    public ResponseEntity<List<ContaCorrenteExtratoDTO>> getExtratoFornecedor(@PathVariable Long id) {
        log.debug("Pedido HTTP GET recebido: /api/contas-correntes/fornecedores/{}/extrato", id);
        return ResponseEntity.ok(service.obterExtratoFornecedor(id));
    }
}