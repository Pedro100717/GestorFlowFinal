package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.*;
import pt.gestorflow.backend.service.TesourariaService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tesouraria")
@RequiredArgsConstructor
public class TesourariaController {

    private final TesourariaService service;

    @GetMapping("/simulador")
    public ResponseEntity<SimuladorTesourariaDTO> obterSimulacao() {
        return ResponseEntity.ok(service.obterSimulacao());
    }

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
    // --- 🚀 A ROTA QUE TE FALTAVA AQUI! ---
    // ==========================================
    @PostMapping("/movimentos")
    public ResponseEntity<MovimentoResponseDTO> registarMovimento(@Valid @RequestBody MovimentoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registarMovimento(dto));
    }

    // ==========================================
    // --- OS NOVOS ENDPOINTS DA TESOURARIA ---
    // ==========================================

    @GetMapping("/pendentes")
    public ResponseEntity<List<DocumentoPendenteDTO>> listarPendentes() {
        return ResponseEntity.ok(service.listarPendentes());
    }

    @PostMapping("/confirmar-pagamento")
    public ResponseEntity<Void> confirmarPagamento(@Valid @RequestBody ConfirmarPagamentoDTO dto) {
        service.confirmarTransacao(dto);
        return ResponseEntity.ok().build();
    }

    // 🚀 ROTA CORRIGIDA!
    @DeleteMapping("/movimentos/{id}")
    public ResponseEntity<Void> anularMovimento(@PathVariable Long id) {
        service.anularMovimento(id); // Agora sim, chama apenas "service"
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/previsao/{tipoDocumento}/{id}")
    public ResponseEntity<Void> atualizarPrevisao(
            @PathVariable String tipoDocumento,
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {

        // O Angular vai enviar um JSON simples: {"novaData": "2026-06-15"}
        String dataStr = payload.get("novaData");

        // 🚀 AGORA SIM: Parsing direto para LocalDate, sem horas!
        LocalDate novaData = LocalDate.parse(dataStr);

        service.atualizarPrevisaoPagamento(id, tipoDocumento, novaData);
        return ResponseEntity.ok().build();
    }
}