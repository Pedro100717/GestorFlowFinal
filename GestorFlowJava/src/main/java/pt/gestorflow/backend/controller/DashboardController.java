package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // 🚀 Documentação OpenAPI
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.gestorflow.backend.dto.DashboardDTO;
import pt.gestorflow.backend.service.DashboardService;

import java.time.LocalDate;

@Slf4j // 🚀 Telemetria de entrada
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Indicadores financeiros e resumo executivo para o ecrã inicial")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Obter Resumo Financeiro", description = "Devolve os KPIs principais (faturação, despesas, saldos). Aceita intervalo de datas opcional para filtragem.")
    @GetMapping("/resumo")
    public ResponseEntity<DashboardDTO> getResumo(
            @RequestParam(value = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(value = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {

        // 🚀 O log capta as datas enviadas (se existirem) para efeitos de análise de performance
        log.debug("Pedido HTTP GET recebido: /api/dashboard/resumo (Início: {}, Fim: {})", inicio, fim);

        return ResponseEntity.ok(dashboardService.getResumo(inicio, fim));
    }
}