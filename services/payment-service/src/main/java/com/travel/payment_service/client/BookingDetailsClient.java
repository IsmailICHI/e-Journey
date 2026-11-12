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
    private final String confirmationSecret;

    public BookingDetailsClient(
            RestClient.Builder builder,
            @Value("${booking-service.base-url}") String baseUrl,
            @Value("${booking-service.confirmation-secret}") String confirmationSecret) {
        restClient = builder.baseUrl(baseUrl).build();
        this.confirmationSecret = confirmationSecret;
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
        String paymentMethod = response.get("paymentMethod") instanceof String value ? value : "PAYPAL";
        String status = response.get("status") instanceof String value ? value : null;
        if (userId == null || travelId == null || price == null || price.doubleValue() <= 0) {
            throw new IllegalStateException("Booking has no valid user, travel, or MAD price");
        }
        return new BookingSnapshot(
                id.longValue(),
                userId.longValue(),
                travelId.longValue(),
                managerId == null ? null : managerId.longValue(),
                String.valueOf(response.get("travelTitle")),
                new BigDecimal(price.toString()),
                paymentMethod,
                status);
    }

    public void confirmPayment(Long bookingId, String authorization) {
        if (confirmationSecret.isBlank()) {
            throw new IllegalStateException(
                    "Booking payment confirmation is not configured: BOOKING_PAYMENT_CONFIRMATION_SECRET is required");
        }
        restClient.post()
                .uri("/api/v1/bookings/payments/{id}/confirm", bookingId)
                .headers(headers -> {
                    headers.set("Authorization", authorization);
                    headers.set("X-Payment-Service-Token", confirmationSecret);
                })
                .retrieve()
                .toBodilessEntity();
    }

    public record BookingSnapshot(
            Long id,
            Long userId,
            Long travelId,
            Long managerId,
            String travelTitle,
            BigDecimal priceMad,
            String paymentMethod,
            String status) {
    }
}
