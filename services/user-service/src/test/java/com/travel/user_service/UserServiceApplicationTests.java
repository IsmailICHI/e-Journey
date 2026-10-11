package com.travel.user_service;

import com.travel.user_service.models.Role;
import com.travel.user_service.models.User;
import com.travel.user_service.services.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@ActiveProfiles("test")
class UserServiceApplicationTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JwtService jwtService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(springSecurity())
				.build();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void adminEndpointRejectsRequestWithoutToken() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void adminEndpointRejectsUserRoleToken() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users")
				.header("Authorization", "Bearer " + tokenFor(Role.User)))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminEndpointAllowsAdminRoleToken() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users")
				.header("Authorization", "Bearer " + tokenFor(Role.Admin)))
				.andExpect(status().isOk());
	}

	private String tokenFor(Role role) {
		User user = new User();
		user.setId(1L);
		user.setEmail("security-test@example.test");
		user.setRole(role);
		return jwtService.generateToken(user);
	}

}
