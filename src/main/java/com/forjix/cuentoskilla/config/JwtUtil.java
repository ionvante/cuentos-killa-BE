package com.forjix.cuentoskilla.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private final String secret;
    private final long expiration = 1000 * 60 * 60 * 5; // 5 horas

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
    }

    private Key getSigningKey() {
        byte[] keyBytes = resolveKeyBytes(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] resolveKeyBytes(String value) {
        // 1) Base64 estandar
        try {
            byte[] decoded = Decoders.BASE64.decode(value);
            if (decoded.length >= 32) {
                return decoded;
            }
        } catch (DecodingException ignored) {
            // continua con otros formatos
        }

        // 2) Base64URL (acepta '-' y '_')
        try {
            byte[] decoded = Decoders.BASE64URL.decode(value);
            if (decoded.length >= 32) {
                return decoded;
            }
        } catch (DecodingException ignored) {
            // continua a texto plano
        }

        // 3) Texto plano: derivar 32 bytes estables con SHA-256
        logger.warn("jwt.secret no es Base64 valido o es corto; se derivara una clave SHA-256 desde texto plano");
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo inicializar SHA-256 para jwt.secret", e);
        }
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
