package com.travel.payment_service.provider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class StripeClient {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;
    private final String secretKey;
    private final String clientBaseUrl;

    public StripeClient(
            RestClient.Builder builder,
            @Value("${stripe.base-url}") String baseUrl,
            @Value("${stripe.secret-key}") String secretKey,
            @Value("${stripe.client-base-url}") String clientBaseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.secretKey = secretKey;
        this.clientBaseUrl = clientBaseUrl.replaceAll("/+$", "");
    }

    public Map<String, Object> createCheckoutSession(
            Long bookingId, String travelTitle, BigDecimal amount, String currency) {
        requireSecretKey();
        long amountMinor = amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "payment");
        form.add("success_url",
                clientBaseUrl + "/payment/" + bookingId + "?stripe_session_id={CHECKOUT_SESSION_ID}");
        form.add("cancel_url", clientBaseUrl + "/payment/" + bookingId + "?payment_cancelled=true");
        form.add("client_reference_id", bookingId.toString());
        form.add("line_items[0][price_data][currency]", currency.toLowerCase(java.util.Locale.ROOT));
        form.add("line_items[0][price_data][unit_amount]", Long.toString(amountMinor));
        form.add("line_items[0][price_data][product_data][name]", travelTitle);
        form.add("line_items[0][quantity]", "1");

        Map<String, Object> session = restClient.post()
                .uri("/v1/checkout/sessions")
                .headers(headers -> headers.setBearerAuth(secretKey))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JSON_MAP);
        if (session == null || !(session.get("id") instanceof String)
                || !(session.get("url") instanceof String)) {
            throw new IllegalStateException("Stripe did not return a checkout session and URL");
        }
        return session;
    }

    public Map<String, Object> getCheckoutSession(String sessionId) {
        requireSecretKey();
        Map<String, Object> session = restClient.get()
                .uri("/v1/checkout/sessions/{sessionId}", sessionId)
                .headers(headers -> headers.setBearerAuth(secretKey))
                .retrieve()
                .body(JSON_MAP);
        if (session == null) {
            throw new IllegalStateException("Stripe returned an empty checkout session");
        }
        return session;
    }

    private void requireSecretKey() {
        if (secretKey.isBlank()) {
            throw new IllegalStateException("Stripe is not configured: STRIPE_SECRET_KEY is required");
        }
    }
}
