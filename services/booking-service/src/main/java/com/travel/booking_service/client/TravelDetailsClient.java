package com.travel.booking_service.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TravelDetailsClient {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;

    public TravelDetailsClient(
            RestClient.Builder builder,
            @Value("${travel-service.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public TravelSnapshot getTravel(Long travelId, String authorization) {
        Map<String, Object> response = restClient.get()
                .uri("/api/v1/places/travels/{id}", travelId)
                .headers(headers -> headers.set("Authorization", authorization))
                .retrieve()
                .body(JSON_MAP);
        if (response == null || !(response.get("travelId") instanceof Number id)) {
            throw new IllegalStateException("Travel service returned an invalid travel record");
        }
        Number price = response.get("priceMad") instanceof Number value ? value : null;
        Number creator = response.get("createdByUserId") instanceof Number value ? value : null;
        Object methodValue = response.get("paymentMethod");
        String paymentMethod = methodValue instanceof String method ? method : null;
        Object startDateValue = response.get("startDate");
        LocalDate departureDate = startDateValue instanceof String date && !date.isBlank()
                ? LocalDate.parse(date)
                : null;
        String city = response.get("city") instanceof String value ? value : null;
        String country = response.get("country") instanceof String value ? value : null;
        Integer duration = response.get("duration") instanceof Number value ? value.intValue() : null;
        java.util.List<String> destinations = response.get("destinations") instanceof java.util.List<?> values
                ? values.stream().filter(String.class::isInstance).map(String.class::cast).toList()
                : java.util.List.of();
        return new TravelSnapshot(
                id.longValue(),
                String.valueOf(response.get("title")),
                price == null ? null : new BigDecimal(price.toString()),
                creator == null ? null : creator.longValue(),
                paymentMethod,
                departureDate,
                city,
                country,
                duration,
                destinations);
    }

    public record TravelSnapshot(
            Long id,
            String title,
            BigDecimal priceMad,
            Long managerId,
            String paymentMethod,
            LocalDate departureDate,
            String city,
            String country,
            Integer duration,
            java.util.List<String> destinations) {
    }
}
