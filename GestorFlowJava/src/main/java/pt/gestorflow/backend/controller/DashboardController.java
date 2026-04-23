package pt.gestorflow.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.gestorflow.backend.dto.DashboardDTO; // 🛡️ NOVO IMPORT DO DTO
import pt.gestorflow.backend.service.DashboardService;

// O import do java.util.Map foi removido porque já não precisamos dele!

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/resumo")
    // 🛡️ CORREÇÃO: O tipo de retorno agora é estritamente o DashboardDTO
    public ResponseEntity<DashboardDTO> getResumo() {
        return ResponseEntity.ok(dashboardService.getResumo());
    }
}