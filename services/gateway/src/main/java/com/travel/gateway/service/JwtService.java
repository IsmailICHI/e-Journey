package com.travel.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret}") String secret) {
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

    public String extractRole(String token) {
        Claims claims = getClaims(token);
        Object roleClaim = claims.get("role");
        if (roleClaim != null) {
            String role = normalizeRoleValue(roleClaim);
            if (role != null) {
                return role;
            }
        }

        Object rolesClaim = claims.get("roles");
        if (rolesClaim != null) {
            return normalizeRoleValue(rolesClaim);
        }

        return null;
    }

    private String normalizeRoleValue(Object roleValue) {
        if (roleValue instanceof String role) {
            return normalizeRole(role);
        }
        if (roleValue instanceof Iterable<?> roles) {
            for (Object role : roles) {
                if (role instanceof String roleName) {
                    String normalized = normalizeRole(roleName);
                    if (normalized != null) {
                        return normalized;
                    }
                }
            }
        } else if (roleValue.getClass().isArray()) {
            for (Object role : (Object[]) roleValue) {
                if (role instanceof String roleName) {
                    String normalized = normalizeRole(roleName);
                    if (normalized != null) {
                        return normalized;
                    }
                }
            }
        }
        return null;
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return null;
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        if (normalizedRole.isBlank()) {
            return null;
        }
        return normalizedRole.startsWith("ROLE_") ? normalizedRole.substring(5) : normalizedRole;
    }
}