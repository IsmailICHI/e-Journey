package com.travel.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.travel.gateway.service.JwtService;
import com.travel.gateway.service.TokenBlacklistService;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String AUTH_LOGIN = "/api/v1/auth/login";
    private static final String AUTH_REGISTER = "/api/v1/auth/register";
    private static final String PUBLIC_PLACES = "/api/v1/places/public";

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(JwtService jwtService, TokenBlacklistService tokenBlacklistService) {
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        System.out.println("[---- JwtAuthenticationFilter: Incoming request path:] " + path);
        System.out.println("[---- JwtAuthenticationFilter: Incoming request URI:] " + request.getURI().toString());
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (HttpMethod.OPTIONS.equals(request.getMethod()) || isPublicEndpoint(request.getMethod(), path)) {
            return chain.filter(exchange);
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Bearer token required");
        }

        String token = authHeader.substring(7);
        if (token.isBlank() || !jwtService.validateToken(token)) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid bearer token");
        }

        String revocationUserId = jwtService.extractUserId(token);
        if (revocationUserId == null || revocationUserId.isBlank()) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "****** has no user identity");
        }

        return tokenBlacklistService.isBlacklisted(token, revocationUserId)
                .materialize()
                .flatMap(isBlacklisted -> {
                    if (isBlacklisted.isOnError()) {
                        return reject(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                                "Authentication service unavailable");
                    }
                    if (Boolean.TRUE.equals(isBlacklisted.get())) {
                        return reject(exchange, HttpStatus.UNAUTHORIZED, "Bearer token has been revoked");
                    }

                    String userId = revocationUserId;
                    String email = jwtService.extractEmail(token);
                    if (userId == null || userId.isBlank()) {
                        return reject(exchange, HttpStatus.UNAUTHORIZED, "Bearer token has no user identity");
                    }

                    String role = jwtService.extractRole(token);
                    if (isAdminEndpoint(path) && !"ADMIN".equals(role)) {
                        return reject(exchange, HttpStatus.FORBIDDEN, "Administrator role required");
                    }

                    ServerHttpRequest mutatedRequest = request.mutate()
                            .headers(headers -> {
                                headers.remove("X-User-Id");
                                headers.remove("X-User-Email");
                                headers.remove("X-User-Role");
                                headers.set("X-User-Id", userId);
                                if (email != null && !email.isBlank()) {
                                    headers.set("X-User-Email", email);
                                }
                                if (role != null && !role.isBlank()) {
                                    headers.set("X-User-Role", role);
                                }
                            })
                            .build();
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }

    private boolean isPublicEndpoint(HttpMethod method, String path) {
        return (HttpMethod.POST.equals(method)
                && (AUTH_LOGIN.equals(path) || AUTH_REGISTER.equals(path)))
                || (HttpMethod.GET.equals(method) && PUBLIC_PLACES.equals(path));
    }

    private boolean isAdminEndpoint(String path) {
        return "/api/v1/admin".equals(path) || path.startsWith("/api/v1/admin/");
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String path = exchange.getRequest().getURI().getPath();
        byte[] body = ("{\"timestamp\":\"" + java.time.Instant.now() + "\",\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"message\":\"" + message
                + "\",\"errorMessage\":\"" + message
                + "\",\"path\":\"" + path + "\"}").getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}