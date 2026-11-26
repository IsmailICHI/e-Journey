package com.travel.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void allowsPatchPreflightFromConfiguredClientOrigin() {
        String origin = "http://localhost:4200";
        CorsConfigurationSource source = new CorsConfig(List.of(origin)).corsConfigurationSource();
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/v1/admin/users/42/role")
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "PATCH")
                        .build());

        CorsConfiguration configuration = source.getCorsConfiguration(exchange);

        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin(origin)).isEqualTo(origin);
        assertThat(configuration.checkHttpMethod(HttpMethod.PATCH)).isNotNull();
        assertThat(configuration.getAllowedMethods()).contains(HttpMethod.PATCH.name());
    }
}
