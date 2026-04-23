package pt.gestorflow.backend.service;

import lombok.RequiredArgsConstructor;
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
}