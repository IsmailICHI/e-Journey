package com.travel.payment_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePaymentRequest(@NotNull @Positive Long bookingId) {
}
