package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // 🚀 Importações para a documentação da API
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.gestorflow.backend.dto.AnaliseAnaliticaDTO;
import pt.gestorflow.backend.service.AnaliseService;

import java.util.List;

@Slf4j // 🚀 Telemetria de entrada
@RestController
@RequestMapping("/api/analise")
@RequiredArgsConstructor
@Tag(name = "Análise Analítica", description = "Endpoints para os gráficos e dados do Dashboard Analítico") // 🚀 Título no Swagger
public class AnaliseController {

    private final AnaliseService analiseService;

    @Operation(summary = "Obter dados do Dashboard", description = "Devolve a lista de análises agregadas para os gráficos da página inicial do utilizador.") // 🚀 Descrição do método
    @GetMapping("/dashboard")
    public ResponseEntity<List<AnaliseAnaliticaDTO>> getDashboardAnalitico() {

        log.debug("Pedido HTTP GET recebido: /api/analise/dashboard");

        // 🛡️ O Controller só lida com o DTO limpo que veio do Service
        List<AnaliseAnaliticaDTO> resultado = analiseService.obterDashboard();

        return ResponseEntity.ok(resultado);
    }
}