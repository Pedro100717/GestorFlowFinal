package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // 🚀 Documentação OpenAPI
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.estatisticas.EstatisticaClienteDTO;
import pt.gestorflow.backend.dto.estatisticas.EstatisticaContaDTO;
import pt.gestorflow.backend.dto.estatisticas.EstatisticaFornecedorDTO;
import pt.gestorflow.backend.service.EstatisticasService;

@Slf4j // 🚀 Telemetria de entrada
@RestController
@RequestMapping("/api/estatisticas")
@RequiredArgsConstructor
@Tag(name = "Estatísticas", description = "Métricas e cálculos agregados para análises financeiras e de parceiros")
public class EstatisticasController {

    private final EstatisticasService service;

    // Link: GET /api/estatisticas/lucro/conta/1
    @Operation(summary = "Lucro por Conta", description = "Calcula o saldo de lucros/prejuízos operacionais associados a uma conta bancária específica.")
    @GetMapping("/lucro/conta/{contaId}")
    public ResponseEntity<EstatisticaContaDTO> obterLucroConta(@PathVariable Long contaId) {
        log.debug("Pedido HTTP GET recebido: /api/estatisticas/lucro/conta/{}", contaId);

        // 🛡️ O Serviço agora devolve o DTO completo, o Controller só o passa para a frente
        return ResponseEntity.ok(service.getLucroDaConta(contaId));
    }

    // Link: GET /api/estatisticas/gastos/fornecedor/5
    @Operation(summary = "Gastos por Fornecedor", description = "Devolve a soma total de despesas e compras associadas a um determinado fornecedor.")
    @GetMapping("/gastos/fornecedor/{fornecedorId}")
    public ResponseEntity<EstatisticaFornecedorDTO> obterGastosFornecedor(@PathVariable Long fornecedorId) {
        log.debug("Pedido HTTP GET recebido: /api/estatisticas/gastos/fornecedor/{}", fornecedorId);

        return ResponseEntity.ok(service.getTotalGastoComFornecedor(fornecedorId));
    }

    // Link: GET /api/estatisticas/recebimentos/cliente/3
    @Operation(summary = "Recebimentos por Cliente", description = "Devolve a soma total de receitas e vendas recebidas de um determinado cliente.")
    @GetMapping("/recebimentos/cliente/{clienteId}")
    public ResponseEntity<EstatisticaClienteDTO> obterRecebimentosCliente(@PathVariable Long clienteId) {
        log.debug("Pedido HTTP GET recebido: /api/estatisticas/recebimentos/cliente/{}", clienteId);

        return ResponseEntity.ok(service.getTotalRecebidoDeCliente(clienteId));
    }
}