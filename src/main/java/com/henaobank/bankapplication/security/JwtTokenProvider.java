package com.henaobank.bankapplication.security;

import com.henaobank.bankapplication.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider (JwtProperties props){
        this.key = Keys.hmacShaKeyFor(decodeSecret(props.secret()));
        this.expirationMs = props.expirationMS();
    }

    /**
     * Acepta un secreto en Base64 (recomendado: openssl rand -base64 48) o texto
     * plano de al menos 32 caracteres. HS256 necesita 256 bits de clave.
     */
    private static byte[] decodeSecret(String secret) {
        String value = secret == null ? "" : secret.trim();
        if (value.isEmpty()) {
            throw new IllegalStateException("El secreto JWT no puede estar vacío.");
        }

        try {
            byte[] decoded = Decoders.BASE64.decode(value);
            if (decoded.length >= MIN_SECRET_BYTES) {
                return decoded;
            }
        } catch (DecodingException ignored) {
            // No es Base64 válido: se trata como texto plano.
        }

        byte[] raw = value.getBytes(StandardCharsets.UTF_8);
        if (raw.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "El secreto JWT debe tener al menos 32 caracteres o ser Base64 de 32+ bytes.");
        }
        return raw;
    }

    public String generateToken(String username){
        Date now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

    }

    public String getUserNameFromJwt(String token){
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();

    }

    public Boolean validateToken(String token){
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e){
            return false;
        }
    }
}
