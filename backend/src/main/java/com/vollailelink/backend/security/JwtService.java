package com.vollailelink.backend.security;

import com.vollailelink.backend.model.Administrator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET doit contenir au moins 32 octets");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Administrator administrator) {
        final long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(administrator.getPhone())
                .claim("adminId", administrator.getId())
                .claim("mustChangePassword", administrator.getMustChangePassword())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token, Administrator administrator) {
        try {
            Claims claims = extractClaims(token);
            return administrator.getPhone().equals(claims.getSubject())
                    && administrator.getStatus() == com.vollailelink.backend.model.enums.AdminStatus.ACTIVE
                    && (administrator.getLockedUntil() == null
                    || !administrator.getLockedUntil().isAfter(java.time.OffsetDateTime.now()));
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
