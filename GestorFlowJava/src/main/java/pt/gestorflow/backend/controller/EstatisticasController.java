package pt.gestorflow.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.service.EstatisticasService;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/estatisticas")
@RequiredArgsConstructor
public class EstatisticasController {

    private final EstatisticasService service;

    // Link: GET /api/estatisticas/lucro/conta/1
    @GetMapping("/lucro/conta/{contaId}")
    public ResponseEntity<Map<String, BigDecimal>> obterLucroConta(@PathVariable Long contaId) {
        BigDecimal lucro = service.getLucroDaConta(contaId);
        return ResponseEntity.ok(Map.of("valor", lucro));
    }

    // Link: GET /api/estatisticas/gastos/fornecedor/5
    @GetMapping("/gastos/fornecedor/{fornecedorId}")
    public ResponseEntity<Map<String, BigDecimal>> obterGastosFornecedor(@PathVariable Long fornecedorId) {
        BigDecimal total = service.getTotalGastoComFornecedor(fornecedorId);
        return ResponseEntity.ok(Map.of("valor", total));
    }

    // Link: GET /api/estatisticas/recebimentos/cliente/3
    @GetMapping("/recebimentos/cliente/{clienteId}")
    public ResponseEntity<Map<String, BigDecimal>> obterRecebimentosCliente(@PathVariable Long clienteId) {
        BigDecimal total = service.getTotalRecebidoDeCliente(clienteId);
        return ResponseEntity.ok(Map.of("valor", total));
    }
}