package pt.gestorflow.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                // Extrai o ID e a Role do Token
                String userId = tokenService.validarToken(token);
                String role = tokenService.extrairRole(token);

                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    Long idDoUtilizador = Long.parseLong(userId);

                    // 🚀 A CORREÇÃO: Transformamos o texto "SUPER_ADMIN" numa Authority "ROLE_SUPER_ADMIN"
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

                    // 🚀 Agora passamos a Authority em vez de uma lista vazia!
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            idDoUtilizador,
                            null,
                            Collections.singletonList(authority)
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