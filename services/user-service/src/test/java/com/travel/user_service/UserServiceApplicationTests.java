package com.travel.user_service;

import com.travel.user_service.models.Role;
import com.travel.user_service.models.User;
import com.travel.user_service.repository.UserRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;


@SpringBootTest
@ActiveProfiles("test")
class UserServiceApplicationTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private UserRepository userRepository;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(springSecurity())
				.build();
	}

	@Test
	void authenticatedUserCanReadTheirOwnProfile() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized());

		User user = new User();
		user.setFullName("Profile Test");
		user.setEmail("profile-" + java.util.UUID.randomUUID() + "@example.test");
		user.setPassword("not-used-by-this-test");
		user.setRole(Role.User);
		user.setState(true);
		user = userRepository.save(user);

		try {
			mockMvc.perform(get("/api/v1/users/me")
					.header("Authorization", "Bearer " + jwtService.generateToken(user)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.fullName").value("Profile Test"))
					.andExpect(jsonPath("$.email").value(user.getEmail()))
					.andExpect(jsonPath("$.role").value("User"))
					.andExpect(jsonPath("$.state").value(true));
		} finally {
			userRepository.deleteById(user.getId());
		}
	}

	@Test
	void adminEndpointEnforcesAuthenticationAndRole() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/admin/users")
				.header("Authorization", "Bearer " + tokenFor(Role.User)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/admin/users")
				.header("Authorization", "Bearer " + tokenFor(Role.Admin)))
				.andExpect(status().isOk());
	}

	@Test
	void onlyAdminsCanDeleteUsersAndCannotCreateOrUpdateThem() throws Exception {
		mockMvc.perform(delete("/api/v1/admin/users/1")
				.header("Authorization", "Bearer " + tokenFor(Role.User)))
				.andExpect(status().isForbidden());
		String adminToken = tokenFor(Role.Admin);
		mockMvc.perform(delete("/api/v1/admin/users/999999")
				.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNotFound());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.post("/api/v1/admin/users")
				.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isMethodNotAllowed());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.put("/api/v1/admin/users/1")
				.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isMethodNotAllowed());

		User admin = new User();
		admin.setFullName("Self Delete " + java.util.UUID.randomUUID());
		admin.setEmail("self-delete-" + java.util.UUID.randomUUID() + "@example.test");
		admin.setPassword("not-used-by-this-test");
		admin.setRole(Role.Admin);
		admin.setState(true);
		admin = userRepository.save(admin);
		try {
			String selfToken = jwtService.generateToken(admin);
			mockMvc.perform(get("/api/v1/admin/users")
					.header("Authorization", "Bearer " + selfToken))
					.andExpect(status().isOk())
					.andExpect(content().string(not(containsString(admin.getEmail()))));
			mockMvc.perform(delete("/api/v1/admin/users/" + admin.getId())
					.header("Authorization", "Bearer " + selfToken))
					.andExpect(status().isForbidden());
			org.assertj.core.api.Assertions.assertThat(userRepository.existsById(admin.getId())).isTrue();
		} finally {
			userRepository.deleteById(admin.getId());
		}
	}

	@Test
	void invalidRegistrationReturnsSpecifiedBadRequestWithValidationErrors() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/register")
				.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
				.content("{\"email\":\"invalid-email\",\"fullName\":\"A\",\"password\":\"123\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.validationErrors.email").exists())
				.andExpect(jsonPath("$.validationErrors.fullName").exists())
				.andExpect(jsonPath("$.validationErrors.password").exists());
	}

	@Test
	void invalidLoginReturnsSpecifiedBadRequestWithValidationErrors() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
				.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
				.content("{\"email\":\"\",\"password\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.validationErrors.email").exists())
				.andExpect(jsonPath("$.validationErrors.password").exists());
	}

	private String tokenFor(Role role) {
		User user = new User();
		user.setId(1L);
		user.setEmail("security-test@example.test");
		user.setRole(role);
		return jwtService.generateToken(user);
	}

}
