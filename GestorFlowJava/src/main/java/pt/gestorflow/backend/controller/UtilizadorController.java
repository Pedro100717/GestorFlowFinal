package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // 🚀 Documentação OpenAPI
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.PerfilResponseDTO;
import pt.gestorflow.backend.service.UtilizadorService; // 🛡️ NOVO SERVIÇO

@Slf4j // 🚀 Telemetria de entrada
@RestController
@RequestMapping("/api/utilizadores")
@RequiredArgsConstructor
@Tag(name = "Utilizadores", description = "Gestão da conta e perfil do utilizador autenticado")
public class UtilizadorController {

    private final UtilizadorService service;

    // 🛡️ CONTRATO DE AÇO: Uma única linha, como manda a boa arquitetura.
    @Operation(summary = "Obter o meu perfil", description = "Devolve a informação de identidade (nome, email, etc.) associada ao token de sessão atual.")
    @GetMapping("/me")
    public ResponseEntity<PerfilResponseDTO> getPerfil() {
        log.debug("Pedido HTTP GET recebido: /api/utilizadores/me");
        return ResponseEntity.ok(service.obterPerfil());
    }
}