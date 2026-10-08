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

import java.util.Set;
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
    void rejectsProtectedRequestWithoutBearerToken() {
        MockServerWebExchange exchange = exchange("/api/v1/users/me", null, null);

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsInvalidBearerToken() {
        jwtService.valid = false;
        MockServerWebExchange exchange = exchange("/api/v1/users/me", "Bearer invalid", null);

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsNonAdminFromAdminRoute() {
        jwtService.roles = Set.of("USER");
        MockServerWebExchange exchange = exchange("/api/v1/admin/users", "Bearer valid", null);

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void forwardsOnlyVerifiedIdentityHeadersForAdmin() {
        jwtService.userId = "admin-1";
        jwtService.email = "admin@example.test";
        jwtService.roles = Set.of("ADMIN");
        MockServerWebExchange exchange = exchange("/api/v1/admin/users", "Bearer valid", "spoofed");
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();

        filter.filter(exchange, forwarded -> {
            forwardedExchange.set(forwarded);
            return Mono.empty();
        }).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-User-Id"))
                .isEqualTo("admin-1");
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-User-Email"))
                .isEqualTo("admin@example.test");
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-User-Roles"))
                .isEqualTo("ADMIN");
    }

    @Test
    void permitsOnlyExpectedPublicMethodAndPath() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/login").build());

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
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
        private Set<String> roles = Set.of();

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
        public Set<String> extractRoles(String token) {
            return roles;
        }
    }

    private static class StubTokenBlacklistService extends TokenBlacklistService {
        private boolean blacklisted;

        StubTokenBlacklistService() {
            super(null);
        }

        @Override
        public Mono<Boolean> isBlacklisted(String token) {
            return Mono.just(blacklisted);
        }
    }
}