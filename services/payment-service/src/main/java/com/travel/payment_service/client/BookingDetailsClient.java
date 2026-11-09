package com.travel.payment_service.client;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BookingDetailsClient {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP =
            new ParameterizedTypeReference<>() { };
    private final RestClient restClient;

    public BookingDetailsClient(
            RestClient.Builder builder,
            @Value("${booking-service.base-url}") String baseUrl) {
        restClient = builder.baseUrl(baseUrl).build();
    }

    public BookingSnapshot getBooking(Long bookingId, String authorization) {
        Map<String, Object> response = restClient.get()
                .uri("/api/v1/bookings/{id}", bookingId)
                .headers(headers -> headers.set("Authorization", authorization))
                .retrieve()
                .body(JSON_MAP);
        if (response == null || !(response.get("id") instanceof Number id)) {
            throw new IllegalStateException("Booking service returned an invalid booking");
        }
        Number userId = (Number) response.get("userId");
        Number travelId = (Number) response.get("travelId");
        Number managerId = response.get("managerId") instanceof Number value ? value : null;
        Number price = response.get("priceMad") instanceof Number value ? value : null;
        if (userId == null || travelId == null || price == null || price.doubleValue() <= 0) {
            throw new IllegalStateException("Booking has no valid user, travel, or MAD price");
        }
        return new BookingSnapshot(
                id.longValue(),
                userId.longValue(),
                travelId.longValue(),
                managerId == null ? null : managerId.longValue(),
                String.valueOf(response.get("travelTitle")),
                new BigDecimal(price.toString()));
    }

    public record BookingSnapshot(
            Long id,
            Long userId,
            Long travelId,
            Long managerId,
            String travelTitle,
            BigDecimal priceMad) {
    }
}
