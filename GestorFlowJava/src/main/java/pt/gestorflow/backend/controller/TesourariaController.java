package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // 🚀 Documentação OpenAPI
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.*;
import pt.gestorflow.backend.service.TesourariaService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j // 🚀 Telemetria de alta segurança ativada
@RestController
@RequestMapping("/api/tesouraria")
@RequiredArgsConstructor
@Tag(name = "Tesouraria e Fluxo de Caixa", description = "Gestão de contas bancárias, transferências, pagamentos e recebimentos")
public class TesourariaController {

    private final TesourariaService service;

    @Operation(summary = "Obter Simulação de Tesouraria", description = "Devolve o saldo atual e a projeção financeira com base nos documentos pendentes.")
    @GetMapping("/simulador")
    public ResponseEntity<SimuladorTesourariaDTO> obterSimulacao() {
        log.debug("Pedido HTTP GET recebido: /api/tesouraria/simulador");
        return ResponseEntity.ok(service.obterSimulacao());
    }

    // 🛡️ 201 CREATED: Standard para quando nasce uma nova conta
    @Operation(summary = "Criar Conta Bancária", description = "Regista uma nova conta bancária ou caixa física no sistema.")
    @PostMapping("/contas")
    public ResponseEntity<ContaBancariaResponseDTO> criarConta(@Valid @RequestBody ContaBancariaDTO dto) {
        log.info("Auditoria de Tesouraria: Nova conta bancária criada (Nome: {})", dto.getNome());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarConta(dto));
    }

    // 🛡️ CONTRATO UNIFICADO: Tudo embrulhado em ResponseEntity
    @Operation(summary = "Listar Contas Bancárias", description = "Devolve todas as contas e caixas associadas à empresa.")
    @GetMapping("/contas")
    public ResponseEntity<List<ContaBancariaResponseDTO>> listarContas() {
        log.debug("Pedido HTTP GET recebido: /api/tesouraria/contas");
        return ResponseEntity.ok(service.listarContas());
    }

    // 🛡️ ADICIONADO: Rota para o Angular abrir os detalhes da conta para edição
    @Operation(summary = "Obter detalhe da Conta", description = "Devolve a informação completa de uma conta bancária específica.")
    @GetMapping("/contas/{id}")
    public ResponseEntity<ContaBancariaResponseDTO> buscarContaPorId(@PathVariable Long id) {
        log.debug("Pedido HTTP GET recebido: /api/tesouraria/contas/{}", id);
        return ResponseEntity.ok(service.buscarContaPorId(id));
    }

    // 🛡️ CONTRATO UNIFICADO
    @Operation(summary = "Ver Extrato da Conta", description = "Devolve o histórico de movimentos realizados numa conta bancária específica.")
    @GetMapping("/contas/{id}/extrato")
    public ResponseEntity<List<MovimentoResponseDTO>> verExtrato(@PathVariable Long id) {
        log.debug("Pedido HTTP GET recebido: /api/tesouraria/contas/{}/extrato", id);
        return ResponseEntity.ok(service.obterExtrato(id));
    }

    // 🛡️ REMOVIDO MAP: Retorna um 200 OK limpo. Menos lixo a trafegar na rede!
    @Operation(summary = "Realizar Transferência Interna", description = "Transfere fundos entre duas contas bancárias da própria empresa.")
    @PostMapping("/transferencias")
    public ResponseEntity<Void> realizarTransferencia(@Valid @RequestBody TransferenciaDTO dto) {
        log.info("Auditoria de Tesouraria: Transferência de {} € solicitada da Conta Origem ID: {} para Conta Destino ID: {}",
                dto.getValor(), dto.getContaOrigemId(), dto.getContaDestinoId());

        service.transferirEntreContas(dto);
        return ResponseEntity.ok().build();
    }

    // ==========================================
    // --- 🚀 A ROTA QUE TE FALTAVA AQUI! ---
    // ==========================================
    @Operation(summary = "Registar Movimento Direto", description = "Regista uma entrada ou saída avulsa numa conta (ex: taxas, comissões).")
    @PostMapping("/movimentos")
    public ResponseEntity<MovimentoResponseDTO> registarMovimento(@Valid @RequestBody MovimentoDTO dto) {
        log.info("Auditoria de Tesouraria: Movimento direto registado. Conta ID: {}, Valor: {}", dto.getContaId(), dto.getValor());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registarMovimento(dto));
    }

    // ==========================================
    // --- OS NOVOS ENDPOINTS DA TESOURARIA ---
    // ==========================================

    @Operation(summary = "Listar Documentos Pendentes", description = "Devolve todas as faturas e compras que aguardam liquidação.")
    @GetMapping("/pendentes")
    public ResponseEntity<List<DocumentoPendenteDTO>> listarPendentes() {
        log.debug("Pedido HTTP GET recebido: /api/tesouraria/pendentes");
        return ResponseEntity.ok(service.listarPendentes());
    }

    @Operation(summary = "Confirmar Pagamento/Recebimento", description = "Liquida um documento pendente (fatura ou compra), injetando o movimento na conta bancária selecionada.")
    @PostMapping("/confirmar-pagamento")
    public ResponseEntity<Void> confirmarPagamento(
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ConfirmarPagamentoDTO dto) {

        log.info("Auditoria de Tesouraria: Liquidação recebida para o Documento Tipo: {}, ID: {}. (Chave de Idempotência: {})",
                dto.getTipoDocumento(), dto.getDocumentoId(), idempotencyKey);

        // 🚀 Passamos a chave para o cérebro da operação!
        service.confirmarTransacao(dto, idempotencyKey);

        return ResponseEntity.ok().build();
    }

    // 🚀 ROTA CORRIGIDA!
    @Operation(summary = "Anular Movimento", description = "Reverte um movimento de tesouraria, reabrindo o documento original caso estivesse liquidado.")
    @DeleteMapping("/movimentos/{id}")
    public ResponseEntity<Void> anularMovimento(@PathVariable Long id) {
        log.warn("Auditoria de Tesouraria - ALERTA: Anulação do movimento ID: {} solicitada.", id);
        service.anularMovimento(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Atualizar Previsão de Pagamento", description = "Altera a data prevista para a liquidação de um documento pendente (útil para gestão de fluxo de caixa).")
    @PatchMapping("/previsao/{tipoDocumento}/{id}")
    public ResponseEntity<Void> atualizarPrevisao(
            @PathVariable String tipoDocumento,
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {

        // O Angular vai enviar um JSON simples: {"novaData": "2026-06-15"}
        String dataStr = payload.get("novaData");

        // 🚀 DEFESA DE BURACOS: Impedir Erro 500 se o Frontend enviar a chave errada
        if (dataStr == null) {
            log.error("Erro na atualização da previsão: O payload não contém a chave 'novaData'.");
            throw new IllegalArgumentException("A chave 'novaData' é obrigatória no corpo do pedido.");
        }

        log.info("Auditoria de Tesouraria: Data de previsão do documento {} (ID: {}) alterada para {}", tipoDocumento, id, dataStr);

        // 🚀 AGORA SIM: Parsing direto para LocalDate com segurança!
        LocalDate novaData = LocalDate.parse(dataStr);

        service.atualizarPrevisaoPagamento(id, tipoDocumento, novaData);
        return ResponseEntity.ok().build();
    }
}