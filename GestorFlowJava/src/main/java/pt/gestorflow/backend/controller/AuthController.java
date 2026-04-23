package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.LoginDTO;
import pt.gestorflow.backend.dto.LoginResponseDTO;
import pt.gestorflow.backend.dto.RegistoDTO;
import pt.gestorflow.backend.service.AuthService;
import pt.gestorflow.backend.service.UtilizadorService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UtilizadorService utilizadorService;

    // 🛡️ CONTRATO BLINDADO: Entra LoginDTO, Sai LoginResponseDTO.
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginDTO dados) {
        return ResponseEntity.ok(authService.login(dados));
    }

    // 🛡️ 201 CREATED: O Angular só quer saber se correu bem (Status 201).
    // Nunca devolver a entidade Utilizador para não vazar a Password!
    @PostMapping("/register")
    public ResponseEntity<Void> registar(@Valid @RequestBody RegistoDTO dados) {
        utilizadorService.registarNovoUtilizador(dados);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}