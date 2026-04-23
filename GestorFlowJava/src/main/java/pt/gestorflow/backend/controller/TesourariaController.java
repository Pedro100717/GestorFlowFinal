package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.*;
import pt.gestorflow.backend.service.TesourariaService;

import java.util.List;

@RestController
@RequestMapping("/api/tesouraria")
@RequiredArgsConstructor
public class TesourariaController {

    private final TesourariaService service;

    // 🛡️ 201 CREATED: Standard para quando nasce uma nova conta
    @PostMapping("/contas")
    public ResponseEntity<ContaBancariaResponseDTO> criarConta(@Valid @RequestBody ContaBancariaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarConta(dto));
    }

    // 🛡️ CONTRATO UNIFICADO: Tudo embrulhado em ResponseEntity
    @GetMapping("/contas")
    public ResponseEntity<List<ContaBancariaResponseDTO>> listarContas() {
        return ResponseEntity.ok(service.listarContas());
    }

    // 🛡️ ADICIONADO: Rota para o Angular abrir os detalhes da conta para edição
    @GetMapping("/contas/{id}")
    public ResponseEntity<ContaBancariaResponseDTO> buscarContaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarContaPorId(id));
    }

    // 🛡️ CONTRATO UNIFICADO
    @GetMapping("/contas/{id}/extrato")
    public ResponseEntity<List<MovimentoResponseDTO>> verExtrato(@PathVariable Long id) {
        return ResponseEntity.ok(service.obterExtrato(id));
    }

    // 🛡️ REMOVIDO MAP: Retorna um 200 OK limpo. Menos lixo a trafegar na rede!
    @PostMapping("/transferencias")
    public ResponseEntity<Void> realizarTransferencia(@Valid @RequestBody TransferenciaDTO dto) {
        service.transferirEntreContas(dto);
        return ResponseEntity.ok().build();
    }

    // ==========================================
    // --- OS NOVOS ENDPOINTS DA TESOURARIA ---
    // ==========================================

    @GetMapping("/pendentes")
    public ResponseEntity<List<DocumentoPendenteDTO>> listarPendentes() {
        // 🛡️ Corrigido de tesourariaService para service
        return ResponseEntity.ok(service.listarPendentes());
    }

    @PostMapping("/confirmar-pagamento")
    public ResponseEntity<Void> confirmarPagamento(@RequestBody ConfirmarPagamentoDTO dto) {
        // 🛡️ Corrigido de tesourariaService para service
        service.confirmarTransacao(dto);
        return ResponseEntity.ok().build();
    }
}