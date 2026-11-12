package com.travel.travel_service.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.travel_service.dto.TravelResponse;

@Service
public class TravelSearchService {

    private static final String INDEX = "travelplan-travels";
    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public TravelSearchService(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${app.elasticsearch.url}") String elasticsearchUrl) {
        this.restClient = builder.baseUrl(elasticsearchUrl).build();
        this.objectMapper = objectMapper;
    }

    public void ensureIndex() {
        try {
            restClient.get().uri("/{index}", INDEX).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound exception) {
            restClient.put()
                    .uri("/{index}", INDEX)
                    .body(Map.of(
                            "settings", Map.of(
                                    "analysis", Map.of(
                                            "analyzer", Map.of(
                                                    "travel_autocomplete", Map.of(
                                                            "tokenizer", "standard",
                                                            "filter", List.of("lowercase", "asciifolding"))))),
                            "mappings", Map.of(
                                    "properties", Map.of(
                                            "title", textField(),
                                            "city", textField(),
                                            "country", textField(),
                                            "description", textField(),
                                            "destinations", textField()))))
                    .retrieve()
                    .toBodilessEntity();
        }
    }

    public void index(TravelResponse travel) {
        ensureIndex();
        restClient.put()
                .uri("/{index}/_doc/{id}", INDEX, travel.travelId())
                .body(travel)
                .retrieve()
                .toBodilessEntity();
    }

    public void delete(Long travelId) {
        ensureIndex();
        try {
            restClient.delete()
                    .uri("/{index}/_doc/{id}", INDEX, travelId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound exception) {
            // Deleting an unindexed travel is already the desired state.
        }
    }

    public List<TravelResponse> search(String query, int limit) {
        ensureIndex();
        Map<String, Object> response = restClient.post()
                .uri("/{index}/_search", INDEX)
                .body(Map.of(
                        "size", Math.max(1, Math.min(limit, 20)),
                        "query", Map.of(
                                "multi_match", Map.of(
                                        "query", query,
                                        "type", "bool_prefix",
                                        "fields", List.of(
                                                "title.suggest^4", "title.suggest._2gram", "title.suggest._3gram",
                                                "city.suggest^3", "city.suggest._2gram",
                                                "country.suggest^2", "country.suggest._2gram",
                                                "destinations.suggest^2", "description.suggest")))))
                .retrieve()
                .body(JSON_MAP);
        if (response == null
                || !(response.get("hits") instanceof Map<?, ?> hits)
                || !(hits.get("hits") instanceof List<?> results)) {
            throw new IllegalStateException("Elasticsearch returned an invalid travel search response");
        }
        List<TravelResponse> travels = new ArrayList<>();
        for (Object result : results) {
            if (result instanceof Map<?, ?> hit && hit.get("_source") != null) {
                travels.add(objectMapper.convertValue(hit.get("_source"), TravelResponse.class));
            }
        }
        return travels;
    }

    private Map<String, Object> textField() {
        return Map.of(
                "type", "search_as_you_type");
    }
}
