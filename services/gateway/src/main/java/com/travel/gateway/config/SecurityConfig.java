package com.travel.gateway.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter,
            CorsConfigurationSource corsConfigurationSource) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/places/public").permitAll()
                        .pathMatchers("/actuator/prometheus", "/actuator/health").permitAll()
                        .pathMatchers("/api/v1/admin", "/api/v1/admin/**").hasRole("ADMIN")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(
            @Value("${jwt.secret}") String secret) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        SecretKey key = new SecretKeySpec(secretBytes, "HmacSHA256");
        MacAlgorithm algorithm = secretBytes.length >= 64
            ? MacAlgorithm.HS512
            : secretBytes.length >= 48 ? MacAlgorithm.HS384 : MacAlgorithm.HS256;
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(key)
            .macAlgorithm(algorithm)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        return decoder;
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            Collection<GrantedAuthority> scopeAuthorities = scopeAuthoritiesConverter.convert(jwt);
            if (scopeAuthorities != null) {
                authorities.addAll(scopeAuthorities);
            }
            addRoleAuthority(jwt.getClaims().get("role"), authorities);
            return authorities;
        });

        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }

    private static void addRoleAuthority(Object roleValue, Collection<GrantedAuthority> authorities) {
        if (!(roleValue instanceof String role)) {
            return;
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        if (normalizedRole.startsWith("ROLE_")) {
            normalizedRole = normalizedRole.substring(5);
        }
        if (!normalizedRole.isBlank()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + normalizedRole));
        }
    }
}