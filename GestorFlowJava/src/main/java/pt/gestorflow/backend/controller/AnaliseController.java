package pt.gestorflow.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.gestorflow.backend.dto.AnaliseAnaliticaDTO;
import pt.gestorflow.backend.service.AnaliseService;

import java.util.List;

@RestController
@RequestMapping("/api/analise")
@RequiredArgsConstructor
public class AnaliseController {

    private final AnaliseService analiseService;

    @GetMapping("/dashboard")
    public ResponseEntity<List<AnaliseAnaliticaDTO>> getDashboardAnalitico() {

        // 🛡️ Agora o Controller só lida com o DTO limpo que veio do Service
        List<AnaliseAnaliticaDTO> resultado = analiseService.obterDashboard();

        return ResponseEntity.ok(resultado);
    }
}