package com.travel.gateway;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"app.rate-limit.enabled=false",
		"jwt.secret=test-secret-key-test-secret-key-test-secret-key"
})
class GatewayApplicationTests {

	@Autowired
	private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter;

	@Test
	void convertsOnlyTheSingleRoleClaimToSpringAuthority() {
		Jwt adminJwt = Jwt.withTokenValue("admin-token")
				.header("alg", "HS256")
				.claim("role", "ADMIN")
				.build();
		Jwt userJwt = Jwt.withTokenValue("user-token")
				.header("alg", "HS256")
				.claim("role", "USER")
				.claim("roles", List.of("ADMIN"))
				.build();

		assertThat(authoritiesFor(adminJwt)).containsExactly("ROLE_ADMIN");
		assertThat(authoritiesFor(userJwt)).containsExactly("ROLE_USER");
	}

	@Test
	void fallsBackToRolesClaimWhenRoleIsAbsent() {
		Jwt rolesOnlyJwt = Jwt.withTokenValue("roles-token")
				.header("alg", "HS256")
				.claim("roles", List.of("TRAVEL_MANAGER"))
				.build();

		assertThat(authoritiesFor(rolesOnlyJwt)).containsExactly("ROLE_TRAVEL_MANAGER");
	}

	private List<String> authoritiesFor(Jwt jwt) {
		return jwtAuthenticationConverter.convert(jwt)
				.block()
				.getAuthorities()
				.stream()
				.map(GrantedAuthority::getAuthority)
				.toList();
	}

}
