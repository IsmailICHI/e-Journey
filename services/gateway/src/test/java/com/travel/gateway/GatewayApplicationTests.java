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
	void contextLoads() {
	}

	@Test
	void convertsUserServiceRoleClaimToSpringRoleAuthority() {
		Jwt jwt = Jwt.withTokenValue("test-token")
				.header("alg", "HS256")
				.claim("role", "ADMIN")
				.build();

		assertThat(authoritiesFor(jwt)).contains("ROLE_ADMIN");
	}

	@Test
	void convertsPluralRoleClaimsToSpringRoleAuthorities() {
		Jwt jwt = Jwt.withTokenValue("test-token")
				.header("alg", "HS256")
				.claim("roles", List.of("USER", "ROLE_ADMIN"))
				.build();

		assertThat(authoritiesFor(jwt)).contains("ROLE_USER", "ROLE_ADMIN");
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
