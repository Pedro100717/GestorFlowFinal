package pt.gestorflow.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.gestorflow.backend.dto.LoginDTO;
import pt.gestorflow.backend.dto.LoginResponseDTO;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.UtilizadorRepository;
import pt.gestorflow.backend.security.TokenService;

@Slf4j // 🚀 Logger ativado
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilizadorRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public LoginResponseDTO login(LoginDTO dados) {
        log.info("Tentativa de login iniciada para o email: {}", dados.getEmail());

        // 1. Vai buscar o utilizador ou lança erro
        Utilizador user = repository.findByEmail(dados.getEmail())
                .orElseThrow(() -> {
                    // 🛡️ Log de segurança antes de disparar o erro para o utilizador
                    log.warn("Falha de segurança: Tentativa de login num email inexistente ({})", dados.getEmail());
                    return new IllegalArgumentException("Credenciais Inválidas.");
                });

        // 2. Compara a password
        if (!passwordEncoder.matches(dados.getSenha(), user.getSenha())) {
            // 🛡️ Registo crítico para detetar ataques de força bruta numa conta que existe
            log.warn("Falha de segurança: Password incorreta para o email ({})", dados.getEmail());
            throw new IllegalArgumentException("Credenciais Inválidas.");
        }

        // 3. Gera o token e o DTO
        String token = tokenService.gerarToken(user);

        log.info("Login realizado com sucesso para o utilizador ID: {}", user.getId());

        return new LoginResponseDTO(token, user.getNomeUtilizador(), user.getEmail());
    }

    public Long getUtilizadorAutenticadoId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // 🚨 Aqui usamos ERROR porque alguém conseguiu passar pelos filtros do Spring Security sem token, o que não devia acontecer!
            log.error("Alerta de Invasão: Tentativa de acesso bloqueada por contexto de segurança vazio ou anónimo.");
            throw new SecurityException("Acesso negado: Utilizador não autenticado no sistema.");
        }

        Object principal = authentication.getPrincipal();

        // 🥷 NINJA: Nenhum log de sucesso aqui para não destruir a performance e o disco do servidor!
        if (principal instanceof Long id) {
            return id;
        }

        if (principal instanceof String idStr) {
            return Long.parseLong(idStr);
        }

        // 🚨 Erro estrutural, o programador mexeu em algo que não devia nos filtros JWT.
        log.error("Erro crítico de arquitetura: O principal no JWT não é Long nem String. Tipo recebido: {}", principal.getClass().getName());
        throw new SecurityException("Erro crítico de arquitetura: O formato do utilizador autenticado é inválido.");
    }
}