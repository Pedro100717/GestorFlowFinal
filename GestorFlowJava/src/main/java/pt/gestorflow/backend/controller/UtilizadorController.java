package pt.gestorflow.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.PerfilResponseDTO;
import pt.gestorflow.backend.service.UtilizadorService; // 🛡️ NOVO SERVIÇO

@RestController
@RequestMapping("/api/utilizadores")
@RequiredArgsConstructor
public class UtilizadorController {

    private final UtilizadorService service;

    // 🛡️ CONTRATO DE AÇO: Uma única linha, como manda a boa arquitetura.
    @GetMapping("/me")
    public ResponseEntity<PerfilResponseDTO> getPerfil() {
        return ResponseEntity.ok(service.obterPerfil());
    }
}