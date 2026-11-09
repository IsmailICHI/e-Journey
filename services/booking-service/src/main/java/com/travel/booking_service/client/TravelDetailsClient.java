package com.travel.booking_service.client;

import java.math.BigDecimal;
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
        return new TravelSnapshot(
                id.longValue(),
                String.valueOf(response.get("title")),
                price == null ? null : new BigDecimal(price.toString()),
                creator == null ? null : creator.longValue());
    }

    public record TravelSnapshot(Long id, String title, BigDecimal priceMad, Long managerId) {
    }
}
