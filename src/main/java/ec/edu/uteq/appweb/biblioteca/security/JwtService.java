package ec.edu.uteq.appweb.biblioteca.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ec.edu.uteq.appweb.biblioteca.domain.Usuario;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * TODO-U4-2: autenticacion JWT stateless. Emite y valida tokens firmados con
 * HMAC-SHA256, con el secreto inyectado por variable de entorno.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMinutos;

    public JwtService(@Value("${app.jwt.secreto}") String secretoBase64,
                       @Value("${app.jwt.expiracion-minutos}") long expiracionMinutos) {
        this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("rol", usuario.getRol().name())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    public String extraerUsername(String token) {
        return parsear(token).getPayload().getSubject();
    }

    public String extraerRol(String token) {
        return parsear(token).getPayload().get("rol", String.class);
    }

    public String extraerJti(String token) {
        return parsear(token).getPayload().getId();
    }

    public boolean esValido(String token) {
        try {
            parsear(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public long expiracionEnSegundos() {
        return expiracionMinutos * 60;
    }

    private io.jsonwebtoken.Jws<io.jsonwebtoken.Claims> parsear(String token) {
        try {
            return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token);
        } catch (ExpiredJwtException ex) {
            throw ex;
        }
    }
}
