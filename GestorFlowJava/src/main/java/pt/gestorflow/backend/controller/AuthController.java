package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.LoginDTO;
import pt.gestorflow.backend.dto.LoginResponseDTO;
import pt.gestorflow.backend.dto.RegistoDTO;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.UtilizadorRepository;
import pt.gestorflow.backend.security.TokenService;
import pt.gestorflow.backend.service.UtilizadorService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UtilizadorRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    // 1. INJEÇÃO DO SERVIÇO DE UTILIZADORES (Para criar a conta)
    private final UtilizadorService utilizadorService;

    // --- LOGIN ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO dados) {
        Optional<Utilizador> userOpt = repository.findByEmail(dados.getEmail());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Credenciais Invalidas.");
        }

        Utilizador user = userOpt.get();

        boolean senhaValida = passwordEncoder.matches(dados.getSenha(), user.getSenha());

        if (!senhaValida) {
            return ResponseEntity.status(401).body("Credenciais Invalidas.");
        }

        String token = tokenService.gerarToken(user);

        // Usamos o nosso DTO para empacotar a resposta (Token, Nome e Email)
        LoginResponseDTO resposta = new LoginResponseDTO(
                token,
                user.getNomeUtilizador(), // Repara que aqui usamos o nome exato da tua Entidade!
                user.getEmail()
        );

        return ResponseEntity.ok(resposta);
    }

    // --- REGISTO (NOVO - O que faltava!) ---
    @PostMapping("/register")
    public ResponseEntity<?> registar(@Valid @RequestBody RegistoDTO dados, BindingResult result) {

        // Valida se o DTO tem erros (ex: email inválido)
        if (result.hasErrors()) {
            Map<String, String> erros = new HashMap<>();
            result.getFieldErrors().forEach(error ->
                    erros.put(error.getField(), error.getDefaultMessage())
            );
            return ResponseEntity.badRequest().body(erros);
        }

        try {
            // Chama o serviço para criar o utilizador na BD
            Utilizador novo = utilizadorService.registarNovoUtilizador(dados);
            return ResponseEntity.ok(novo);
        } catch (RuntimeException e) {
            // Se der erro (ex: Email duplicado), devolve mensagem
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}