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
    // Podes até remover o UtilizadorRepository daqui, já não é preciso para nada!

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                // Extrai o ID que vem cravado no Token
                String userId = tokenService.validarToken(token);

                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    // 🚀 A FORMA CORRETA: O Principal é apenas o ID primitivo!
                    Long idDoUtilizador = Long.parseLong(userId);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            idDoUtilizador, // <-- O ID passa a ser o dono do bilhete
                            null,
                            new ArrayList<>()
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                System.out.println("Erro de Token: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}