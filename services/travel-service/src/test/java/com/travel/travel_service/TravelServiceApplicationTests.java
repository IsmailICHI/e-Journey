package com.travel.travel_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.travel_service.dto.ItineraryRequest;
import com.travel.travel_service.dto.TravelRequest;
import com.travel.travel_service.repository.TravelRepository;
import com.travel.travel_service.services.TravelService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class TravelServiceApplicationTests {

	@Autowired
	private TravelService travelService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private TravelRepository travelRepository;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(springSecurity())
				.build();
	}

	@Test
	void travelResponseSerializesActivitiesAfterServiceTransactionEnds() throws Exception {
		var travel = travelService.getTravel(1L).orElseThrow();

		String json = objectMapper.writeValueAsString(travel);

		assertThat(json).contains("Eiffel Tower");
	}

	@Test
	void adminTravelEndpointsRequireAdministratorRole() throws Exception {
		mockMvc.perform(get("/api/v1/admin/travels"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/admin/travels")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.put("/api/v1/admin/travels/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.delete("/api/v1/admin/travels/1"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/admin/travels")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
						.jwt().jwt(jwt -> jwt.claim("role", "USER"))))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/admin/travels")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
						.jwt().jwt(jwt -> jwt.claim("role", "USER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.put("/api/v1/admin/travels/1")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
						.jwt().jwt(jwt -> jwt.claim("role", "USER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.delete("/api/v1/admin/travels/1")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
						.jwt().jwt(jwt -> jwt.claim("role", "USER"))))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void travelCrudReplacesAndCascadesItineraryAndDestinations() {
		TravelRequest request = new TravelRequest(
				"Coastal escape", "Essaouira", "Morocco", "Atlantic coast", null, 4,
				List.of("Sidi Kaouki"),
				java.time.LocalDate.parse("2026-11-01"),
				java.time.LocalDate.parse("2026-11-04"),
				"Medina riad", "Train and taxi", "Stripe",
				List.of(new ItineraryRequest(1, "Arrival", "Explore the medina", List.of("Harbor walk"))));
		var created = travelService.createTravel(request);
		assertThat(created.travelId()).isNotNull();
		assertThat(created.itinerary()).hasSize(1);

		TravelRequest update = new TravelRequest(
				"Updated coastal escape", "Essaouira", "Morocco", "Updated details", null, 3,
				List.of("Taghazout"), null, null, "Ocean hotel", "Bus", "PayPal",
				List.of(new ItineraryRequest(1, "Surf", "Surf lesson", List.of("Morning surf"))));
		var updated = travelService.updateTravel(created.travelId(), update).orElseThrow();
		assertThat(updated.title()).isEqualTo("Updated coastal escape");
		assertThat(updated.paymentMethod()).isEqualTo("PayPal");
		assertThat(updated.destinations()).containsExactly("Taghazout");
		assertThat(updated.itinerary()).hasSize(1);
		assertThat(updated.itinerary().getFirst().activities()).containsExactly("Morning surf");

		assertThat(travelService.deleteTravel(created.travelId())).isTrue();
		assertThat(travelRepository.findById(created.travelId())).isEmpty();
	}

}
