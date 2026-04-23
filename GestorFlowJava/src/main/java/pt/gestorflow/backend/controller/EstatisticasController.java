package pt.gestorflow.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.estatisticas.EstatisticaClienteDTO;
import pt.gestorflow.backend.dto.estatisticas.EstatisticaContaDTO;
import pt.gestorflow.backend.dto.estatisticas.EstatisticaFornecedorDTO;
import pt.gestorflow.backend.service.EstatisticasService;

// Removemos os imports do java.util.Map e java.math.BigDecimal porque os DTOs tratam disso!

@RestController
@RequestMapping("/api/estatisticas")
@RequiredArgsConstructor
public class EstatisticasController {

    private final EstatisticasService service;

    // Link: GET /api/estatisticas/lucro/conta/1
    @GetMapping("/lucro/conta/{contaId}")
    public ResponseEntity<EstatisticaContaDTO> obterLucroConta(@PathVariable Long contaId) {
        // 🛡️ O Serviço agora devolve o DTO completo, o Controller só o passa para a frente
        return ResponseEntity.ok(service.getLucroDaConta(contaId));
    }

    // Link: GET /api/estatisticas/gastos/fornecedor/5
    @GetMapping("/gastos/fornecedor/{fornecedorId}")
    public ResponseEntity<EstatisticaFornecedorDTO> obterGastosFornecedor(@PathVariable Long fornecedorId) {
        return ResponseEntity.ok(service.getTotalGastoComFornecedor(fornecedorId));
    }

    // Link: GET /api/estatisticas/recebimentos/cliente/3
    @GetMapping("/recebimentos/cliente/{clienteId}")
    public ResponseEntity<EstatisticaClienteDTO> obterRecebimentosCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(service.getTotalRecebidoDeCliente(clienteId));
    }
}