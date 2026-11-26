package com.travel.payment_service.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePaymentRequest(
        @NotNull @Positive Long bookingId,
        @Pattern(regexp = "(?i)PAYPAL|STRIPE") String provider,
        boolean restart) {
}
