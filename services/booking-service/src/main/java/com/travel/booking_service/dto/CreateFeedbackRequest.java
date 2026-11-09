package com.travel.booking_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateFeedbackRequest(
        @NotNull @Positive Long bookingId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotBlank @Size(max = 2000) String comment) {
}
