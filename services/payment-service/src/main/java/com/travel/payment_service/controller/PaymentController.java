package com.travel.payment_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.travel.payment_service.dto.CreatePaymentRequest;
import com.travel.payment_service.dto.PaymentAnalyticsResponse;
import com.travel.payment_service.dto.PaymentOrderResponse;
import com.travel.payment_service.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentOrderResponse createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createOrder(
                userId(jwt), request.bookingId(), "Bearer " + jwt.getTokenValue(), request.restart());
    }

    @PostMapping("/payments/{id}/capture")
    public PaymentOrderResponse capture(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        return paymentService.capture(userId(jwt), id, "Bearer " + jwt.getTokenValue());
    }

    @PostMapping("/payments/stripe/complete")
    public PaymentOrderResponse completeStripeSession(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody StripeSessionRequest request) {
        if (request.sessionId() == null || request.sessionId().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Stripe checkout session ID is required");
        }
        return paymentService.completeStripeSession(
                userId(jwt), request.sessionId(), "Bearer " + jwt.getTokenValue());
    }

    @GetMapping("/admin/payments/history")
    @PreAuthorize("hasRole('ADMIN')")
    public java.util.List<PaymentOrderResponse> paymentHistory() {
        return paymentService.getPaymentHistory();
    }

    @GetMapping("/admin/payments/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentAnalyticsResponse analytics(@RequestParam(defaultValue = "3") int months) {
        return paymentService.getAnalytics(months);
    }

    @GetMapping("/manager/payments/analytics")
    @PreAuthorize("hasRole('TRAVEL_MANAGER')")
    public PaymentAnalyticsResponse managerAnalytics(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "3") int months) {
        return paymentService.getManagerAnalytics(userId(jwt), months);
    }

    public record StripeSessionRequest(String sessionId) {
    }

    private Long userId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (!(claim instanceof Number userId) || userId.longValue() <= 0) {
            throw new IllegalStateException("Authenticated token does not contain a valid user ID");
        }
        return userId.longValue();
    }
}
