package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // 🚀 Documentação OpenAPI
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.LoginDTO;
import pt.gestorflow.backend.dto.LoginResponseDTO;
import pt.gestorflow.backend.dto.RegistoDTO;
import pt.gestorflow.backend.service.AuthService;
import pt.gestorflow.backend.service.UtilizadorService;

@Slf4j // 🚀 Telemetria ativada com segurança
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints públicos para login e registo na plataforma GestorFlow")
public class AuthController {

    private final AuthService authService;
    private final UtilizadorService utilizadorService;

    // 🛡️ CONTRATO BLINDADO: Entra LoginDTO, Sai LoginResponseDTO.
    @Operation(summary = "Efetuar Login", description = "Valida as credenciais do utilizador e gera o Token JWT para acesso seguro às APIs.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginDTO dados) {
        // 🚨 NUNCA fazer log do objeto 'dados' aqui para não gravar passwords nos logs do servidor!
        log.info("Pedido HTTP POST recebido: /api/auth/login");

        return ResponseEntity.ok(authService.login(dados));
    }

    // 🛡️ 201 CREATED: O Angular só quer saber se correu bem (Status 201).
    @Operation(summary = "Registar nova conta", description = "Cria um novo utilizador no sistema. Não devolve o objeto criado na resposta por motivos de segurança.")
    @PostMapping("/register")
    public ResponseEntity<Void> registar(@Valid @RequestBody RegistoDTO dados) {
        // 🛡️ Aqui podemos gravar apenas o email para efeitos de auditoria de tentativas de registo
        log.info("Pedido HTTP POST recebido: /api/auth/register (Email providenciado: {})", dados.getEmail());

        utilizadorService.registarNovoUtilizador(dados);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}