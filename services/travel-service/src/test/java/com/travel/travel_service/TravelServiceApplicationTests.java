package com.travel.travel_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.travel_service.services.TravelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TravelServiceApplicationTests {

	@Autowired
	private TravelService travelService;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void contextLoads() {
	}

	@Test
	void travelResponseSerializesActivitiesAfterServiceTransactionEnds() throws Exception {
		var travel = travelService.getTravel(1L).orElseThrow();

		String json = objectMapper.writeValueAsString(travel);

		assertThat(json).contains("Eiffel Tower");
	}

}
