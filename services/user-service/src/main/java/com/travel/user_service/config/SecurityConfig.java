package com.travel.user_service.config;

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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                .requestMatchers("/api/v1/admin", "/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/users/**").authenticated()
                .anyRequest().permitAll())
            .oauth2ResourceServer(resourceServer -> resourceServer
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        SecretKey key = new SecretKeySpec(secretBytes, "HmacSHA256");
        MacAlgorithm algorithm = secretBytes.length >= 64
            ? MacAlgorithm.HS512
            : secretBytes.length >= 48 ? MacAlgorithm.HS384 : MacAlgorithm.HS256;
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
            .macAlgorithm(algorithm)
            .build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        return decoder;
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            Collection<GrantedAuthority> scopes = scopeAuthoritiesConverter.convert(jwt);
            if (scopes != null) {
                authorities.addAll(scopes);
            }
            addRoleAuthorities(jwt.getClaims().get("roles"), authorities);
            addRoleAuthorities(jwt.getClaims().get("role"), authorities);
            return authorities;
        });
        return converter;
    }

    private static void addRoleAuthorities(Object roleClaim, Collection<GrantedAuthority> authorities) {
        if (roleClaim instanceof Collection<?> roleValues) {
            roleValues.forEach(roleValue -> addRoleAuthority(roleValue, authorities));
        } else {
            addRoleAuthority(roleClaim, authorities);
        }
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
