package com.travel.booking_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateBookingRequest(
        @NotNull(message = "Travel ID is required")
        @Positive(message = "Travel ID must be positive")
        Long travelId) {
}
