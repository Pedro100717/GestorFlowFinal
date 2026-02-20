package pt.gestorflow.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.UtilizadorRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UtilizadorRepository repository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Tenta apanhar o token do cabeçalho "Authorization"
        String header = request.getHeader("Authorization"); // Ex: "Bearer eyJhbGci..."

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // Remove o "Bearer " inicial

            try {
                // 2. Valida o token e extrai o ID do utilizador
                String userId = tokenService.validarToken(token);

                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 3. Vai buscar o utilizador à BD para confirmar que ainda existe
                    Optional<Utilizador> userOpt = repository.findById(Long.parseLong(userId));

                    if (userOpt.isPresent()) {
                        Utilizador user = userOpt.get();

                        // 4. Cria a "Ficha de Entrada" oficial do Spring Security
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                new ArrayList<>() // Aqui iriam as permissões (ADMIN, USER, etc)
                        );

                        // 5. Diz ao Spring: "Este gajo está autenticado!"
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                // Token inválido ou expirado - Não fazemos nada, o Spring vai bloquear a seguir
                System.out.println("Erro de Token: " + e.getMessage());
            }
        }

        // 6. Continua para o próximo passo (ir para o Controller ou bloquear)
        filterChain.doFilter(request, response);
    }
}