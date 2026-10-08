package com.travel.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret:defaultsecretkeydefaultsecretkeydefaultsecretkey}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Extracts userId from Claims set by Auth Service
    public String extractUserId(String token) {
        Object userId = getClaims(token).get("userId");
        return userId != null ? userId.toString() : null;
    }

    // Extracts email from Claims set by Auth Service
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public Set<String> extractRoles(String token) {
        Claims claims = getClaims(token);
        Object roleClaim = claims.get("roles");
        if (roleClaim == null) {
            roleClaim = claims.get("role");
        }

        Set<String> roles = new LinkedHashSet<>();
        if (roleClaim instanceof Collection<?> values) {
            values.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(this::normalizeRole)
                    .forEach(roles::add);
        } else if (roleClaim instanceof String role) {
            roles.add(normalizeRole(role));
        }
        return Set.copyOf(roles);
    }

    private String normalizeRole(String role) {
        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        return normalizedRole.startsWith("ROLE_") ? normalizedRole.substring(5) : normalizedRole;
    }
}