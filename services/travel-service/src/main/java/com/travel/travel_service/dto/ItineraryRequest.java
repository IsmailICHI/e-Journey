package com.travel.travel_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ItineraryRequest(
        @NotNull(message = "Day number is required")
        @Positive(message = "Day number must be greater than 0")
        @Max(value = 365, message = "Day number cannot exceed 365")
        Integer dayNumber,

        @NotBlank(message = "Itinerary title is required")
        @Size(min = 2, max = 120, message = "Itinerary title must be between 2 and 120 characters")
        String title,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,

        @Size(max = 50, message = "Cannot list more than 50 activities per day")
        List<@NotBlank(message = "Activity description cannot be blank") @Size(max = 255, message = "Activity description cannot exceed 255 characters") String> activities) {
}
