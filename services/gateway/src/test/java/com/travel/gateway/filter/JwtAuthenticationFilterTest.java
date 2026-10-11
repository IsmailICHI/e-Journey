package com.travel.gateway.filter;

import com.travel.gateway.service.JwtService;
import com.travel.gateway.service.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private final StubJwtService jwtService = new StubJwtService();
    private final StubTokenBlacklistService tokenBlacklistService = new StubTokenBlacklistService();
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, tokenBlacklistService);
    }

    @Test
    void rejectsMissingInvalidAndNonAdminCredentials() {
        MockServerWebExchange missingToken = exchange("/api/v1/users/me", null, null);
        filter.filter(missingToken, ignored -> Mono.empty()).block();
        assertThat(missingToken.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        jwtService.valid = false;
        MockServerWebExchange invalidToken = exchange("/api/v1/users/me", "Bearer invalid-token", null);
        filter.filter(invalidToken, ignored -> Mono.empty()).block();
        assertThat(invalidToken.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        jwtService.valid = true;
        jwtService.role = "USER";
        MockServerWebExchange nonAdmin = exchange("/api/v1/admin/users", "Bearer test-token", null);
        filter.filter(nonAdmin, ignored -> Mono.empty()).block();
        assertThat(nonAdmin.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void forwardsVerifiedIdentityAndPermitsPublicAndCorsPreflightRequests() {
        jwtService.userId = "admin-1";
        jwtService.email = "admin@example.test";
        jwtService.role = "ADMIN";
        MockServerWebExchange adminExchange = exchange("/api/v1/admin/users", "Bearer test-token", "spoofed");
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();

        filter.filter(adminExchange, forwarded -> {
            forwardedExchange.set(forwarded);
            return Mono.empty();
        }).block();

        assertThat(adminExchange.getResponse().getStatusCode()).isNull();
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-User-Id"))
                .isEqualTo("admin-1");
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-User-Email"))
                .isEqualTo("admin@example.test");
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-User-Role"))
                .isEqualTo("ADMIN");

        MockServerWebExchange publicRequest = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/login").build());
        filter.filter(publicRequest, ignored -> Mono.empty()).block();
        assertThat(publicRequest.getResponse().getStatusCode()).isNull();

        MockServerWebExchange preflightRequest = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/v1/admin/travels")
                        .header("Origin", "http://127.0.0.1:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .build());
        filter.filter(preflightRequest, ignored -> Mono.empty()).block();
        assertThat(preflightRequest.getResponse().getStatusCode()).isNull();
    }

    private MockServerWebExchange exchange(String path, String authorization, String spoofedUserId) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get(path);
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        if (spoofedUserId != null) {
            builder.header("X-User-Id", spoofedUserId);
        }
        return MockServerWebExchange.from(builder.build());
    }

    private static class StubJwtService extends JwtService {
        private boolean valid = true;
        private String userId = "user-1";
        private String email = "user@example.test";
        private String role;

        StubJwtService() {
            super("0123456789abcdef0123456789abcdef");
        }

        @Override
        public boolean validateToken(String token) {
            return valid;
        }

        @Override
        public String extractUserId(String token) {
            return userId;
        }

        @Override
        public String extractEmail(String token) {
            return email;
        }

        @Override
        public String extractRole(String token) {
            return role;
        }
    }

    private static class StubTokenBlacklistService extends TokenBlacklistService {
        StubTokenBlacklistService() {
            super(null, true);
        }

        @Override
        public Mono<Boolean> isBlacklisted(String token) {
            return Mono.just(false);
        }
    }
}
