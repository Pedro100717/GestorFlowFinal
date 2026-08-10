package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // 🚀 Documentação OpenAPI
import jakarta.validation.Valid; // 🚀 O escudo protetor regressou
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.MovimentoPlaneadoDTO;
import pt.gestorflow.backend.service.PlaneamentoService;

import java.time.LocalDate;
import java.util.List;

@Slf4j // 🚀 Telemetria de entrada
@RestController
@RequestMapping("/api/planeamento")
@RequiredArgsConstructor
@Tag(name = "Planeamento e Recorrências", description = "Gestão de despesas e receitas recorrentes (estilo assinaturas)")
public class PlaneamentoController {

    private final PlaneamentoService planeamentoService;

    // 🛡️ CORREÇÃO: @Valid adicionado e retorno 201 CREATED
    @Operation(summary = "Criar Plano Recorrente", description = "Cria uma nova série de movimentos planeados (ex: Renda mensal, avenças).")
    @PostMapping
    public ResponseEntity<MovimentoPlaneadoDTO> criarPlano(@Valid @RequestBody MovimentoPlaneadoDTO dto) {
        log.info("Auditoria de Planeamento: Novo plano recorrente criado.");
        return ResponseEntity.status(HttpStatus.CREATED).body(planeamentoService.criarPlano(dto));
    }

    @Operation(summary = "Listar Planos", description = "Devolve todos os planos recorrentes ativos da empresa.")
    @GetMapping
    public ResponseEntity<List<MovimentoPlaneadoDTO>> listarPlanos() {
        log.debug("Pedido HTTP GET recebido: /api/planeamento");
        return ResponseEntity.ok(planeamentoService.listarPlanos());
    }

    // 🛡️ CORREÇÃO: @Valid adicionado
    @Operation(summary = "Atualizar Plano", description = "Edita as regras ou valores da série recorrente.")
    @PutMapping("/{id}")
    public ResponseEntity<MovimentoPlaneadoDTO> atualizarPlano(@PathVariable Long id, @Valid @RequestBody MovimentoPlaneadoDTO dto) {
        log.debug("Pedido HTTP PUT recebido: /api/planeamento/{}", id);
        return ResponseEntity.ok(planeamentoService.atualizarPlano(id, dto));
    }

    @Operation(summary = "Apagar Plano", description = "Elimina a série recorrente por completo.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagarPlano(@PathVariable Long id) {
        log.info("Auditoria de Planeamento: Plano recorrente ID: {} foi eliminado.", id);
        planeamentoService.apagarPlano(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Pausar/Ativar Plano", description = "Alterna o estado do plano entre ativo e suspenso.")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Void> alternarStatus(@PathVariable Long id) {
        log.info("Auditoria de Planeamento: Estado do Plano ID: {} foi alternado (Toggle)", id);
        planeamentoService.alternarStatus(id);
        return ResponseEntity.ok().build();
    }

    // =========================================================================
    // --- MÁQUINA DO TEMPO: EXCEÇÕES (ESTILO GOOGLE CALENDAR) 🚀 ---
    // =========================================================================

    // 🚀 1. APAGAR APENAS UM MÊS (EXCEÇÃO)
    @Operation(summary = "Ignorar Ocorrência", description = "Remove apenas a ocorrência de uma data específica sem apagar a série.")
    @DeleteMapping("/{id}/excecao")
    public ResponseEntity<Void> ignorarDataDoPlano(
            @PathVariable Long id,
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAignorar) {

        log.info("Auditoria de Planeamento: Ocorrência do dia {} ignorada no Plano ID: {}", dataAignorar, id);
        planeamentoService.ignorarDataPlano(id, dataAignorar);
        return ResponseEntity.noContent().build();
    }

    // 🚀 2. EDITAR APENAS UM MÊS (EXCEÇÃO - SUBSTITUIÇÃO)
    // 🛡️ CORREÇÃO: @Valid adicionado e retorno 201 CREATED
    @Operation(summary = "Criar Exceção na Série", description = "Altera os valores ou regras de apenas uma ocorrência específica da série.")
    @PostMapping("/{id}/excecao")
    public ResponseEntity<MovimentoPlaneadoDTO> criarExcecao(
            @PathVariable Long id,
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataOriginal,
            @Valid @RequestBody MovimentoPlaneadoDTO dtoNovo) {

        log.info("Auditoria de Planeamento: Criada exceção para o dia {} no Plano ID: {}", dataOriginal, id);
        MovimentoPlaneadoDTO excecaoCriada = planeamentoService.criarExcecaoPlano(id, dataOriginal, dtoNovo);

        return ResponseEntity.status(HttpStatus.CREATED).body(excecaoCriada);
    }
}