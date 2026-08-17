package pt.gestorflow.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.BugReportDTO;
import pt.gestorflow.backend.dto.TicketResponseDTO;
import pt.gestorflow.backend.model.ReportSuporte;
import pt.gestorflow.backend.repository.ReportSuporteRepository;
import pt.gestorflow.backend.service.SuporteService;

import java.util.List;

@RestController
@RequestMapping("/api/suporte")
@RequiredArgsConstructor
public class SuporteController {

    private final SuporteService suporteService;

    // 1. O endpoint que os utilizadores normais (e tu) usam no Modal para enviar tickets
    @PostMapping
    public ResponseEntity<String> submeterTicket(@RequestBody BugReportDTO dto) {
        suporteService.processarNovoTicket(dto);
        return ResponseEntity.ok("Ticket submetido com sucesso.");
    }

    // 🚀 2. O NOVO ENDPOINT PARA O BACKOFFICE (Protegido para Super Admins!)
    @GetMapping("/tickets")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<TicketResponseDTO>> listarTodosOsTickets() {
        return ResponseEntity.ok(suporteService.listarTodosTickets());
    }

    // Adiciona este endpoint no teu SuporteController.java
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> apagarTicket(@PathVariable Long id) {
        suporteService.apagarTicket(id);
        return ResponseEntity.noContent().build();
    }
}