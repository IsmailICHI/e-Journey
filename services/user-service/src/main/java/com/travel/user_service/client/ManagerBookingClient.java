package com.travel.user_service.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ManagerBookingClient {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;

    public ManagerBookingClient(
            @Value("${booking-service.base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public Long getTravelerId(Long bookingId, String authorization) {
        Map<String, Object> response = restClient.get()
                .uri("/api/v1/bookings/manager/bookings/{bookingId}", bookingId)
                .headers(headers -> headers.set("Authorization", authorization))
                .retrieve()
                .body(JSON_MAP);
        if (response == null || !(response.get("userId") instanceof Number userId)) {
            throw new IllegalStateException("Booking service returned an invalid manager booking");
        }
        return userId.longValue();
    }
}
