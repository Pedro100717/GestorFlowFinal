package pt.gestorflow.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.model.Utilizador;

import java.security.Key;
import java.util.Date;

@Service
public class TokenService {

    private final Key key;

    // A MAGIA ACONTECE AQUI:
    // O @Value vai ler a linha "jwt.secret=..." do teu application.properties
    // e injetar esse texto na variável 'secret' quando o servidor iniciar.
    public TokenService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String gerarToken(Utilizador utilizador) {
        // Validade do Token: 24 horas (em milissegundos)
        long tempoExpiracao = 24 * 60 * 60 * 1000;
        Date agora = new Date();
        Date dataExpiracao = new Date(agora.getTime() + tempoExpiracao);

        return Jwts.builder()
                .setSubject(utilizador.getId().toString()) // Quem é o dono da pulseira (ID)
                .claim("email", utilizador.getEmail())     // Informação extra
                .setIssuedAt(agora)                        // Quando foi emitida
                .setExpiration(dataExpiracao)              // Quando caduca
                .signWith(key, SignatureAlgorithm.HS256)   // Assinatura digital
                .compact();
    }

    // Validar se o token é fidedigno
    public String validarToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject(); // Retorna o ID do utilizador
    }
}