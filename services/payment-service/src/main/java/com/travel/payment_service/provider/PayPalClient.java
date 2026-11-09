package com.travel.payment_service.provider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class PayPalClient {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public PayPalClient(
            RestClient.Builder restClientBuilder,
            @Value("${paypal.base-url}") String baseUrl,
            @Value("${paypal.client-id}") String clientId,
            @Value("${paypal.client-secret}") String clientSecret) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public Map<String, Object> createOrder(Long bookingId, BigDecimal amount, String currency) {
        String accessToken = accessToken();
        Map<String, Object> amountDetails = Map.of(
                "currency_code", currency,
                "value", amount.toPlainString());
        Map<String, Object> purchaseUnit = Map.of(
                "reference_id", bookingId.toString(),
                "amount", amountDetails);
        Map<String, Object> order = restClient.post()
                .uri("/v2/checkout/orders")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("intent", "CAPTURE", "purchase_units", List.of(purchaseUnit)))
                .retrieve()
                .body(JSON_MAP);
        if (order == null || !(order.get("id") instanceof String)) {
            throw new IllegalStateException("PayPal did not return an order ID");
        }
        return order;
    }

    public Map<String, Object> captureOrder(String orderId) {
        Map<String, Object> capture = restClient.post()
                .uri("/v2/checkout/orders/{orderId}/capture", orderId)
                .headers(headers -> headers.setBearerAuth(accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of())
                .retrieve()
                .body(JSON_MAP);
        if (capture == null) {
            throw new IllegalStateException("PayPal returned an empty capture response");
        }
        return capture;
    }

    private String accessToken() {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        Map<String, Object> response = restClient.post()
                .uri("/v1/oauth2/token")
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JSON_MAP);
        Object token = response == null ? null : response.get("access_token");
        if (!(token instanceof String accessToken) || accessToken.isBlank()) {
            throw new IllegalStateException("PayPal did not return an access token");
        }
        return accessToken;
    }
}
