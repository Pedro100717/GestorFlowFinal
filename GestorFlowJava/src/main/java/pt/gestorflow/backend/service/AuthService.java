package pt.gestorflow.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.gestorflow.backend.dto.LoginDTO;
import pt.gestorflow.backend.dto.LoginResponseDTO;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.UtilizadorRepository;
import pt.gestorflow.backend.security.TokenService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilizadorRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public LoginResponseDTO login(LoginDTO dados) {
        // 1. Vai buscar o utilizador ou lança erro (o GlobalExceptionHandler transforma em 400 Bad Request)
        Utilizador user = repository.findByEmail(dados.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciais Inválidas."));

        // 2. Compara a password
        if (!passwordEncoder.matches(dados.getSenha(), user.getSenha())) {
            throw new IllegalArgumentException("Credenciais Inválidas.");
        }

        // 3. Gera o token e o DTO
        String token = tokenService.gerarToken(user);
        return new LoginResponseDTO(token, user.getNomeUtilizador(), user.getEmail());
    }

    public Long getUtilizadorAutenticadoId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new SecurityException("Acesso negado: Utilizador não autenticado no sistema.");
        }

        Object principal = authentication.getPrincipal();

        // Como o nosso JwtAuthenticationFilter agora injeta um Long puro, a leitura é instantânea e 100% segura
        if (principal instanceof Long id) {
            return id;
        }

        // Prevenção extra caso o ID venha como String do JWT nalgum momento
        if (principal instanceof String idStr) {
            return Long.parseLong(idStr);
        }

        throw new SecurityException("Erro crítico de arquitetura: O formato do utilizador autenticado é inválido.");
    }
}