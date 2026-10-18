package com.travel.travel_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record TravelRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 20, message = "Title must be between 3 and 20 characters")
        @Pattern (regexp = "^[A-Za-z0-9\\s]+$", message = "Title can only contain letters, numbers, and spaces")
        String title,

        @NotBlank(message = "City is required")
        @Size(min = 2, max = 20, message = "City must be between 2 and 20 characters")
        @Pattern (regexp = "^[A-Za-z\\s]+$", message = "City can only contain letters and spaces")
        String city,

        @NotBlank(message = "Country is required")
        @Size(min = 2, max = 20, message = "Country must be between 2 and 20 characters")
        @Pattern (regexp = "^[A-Za-z\\s]+$", message = "Country can only contain letters and spaces")
        String country,

        @Size(max = 200, message = "Description cannot exceed 200 characters")
        @Pattern (regexp = "^[A-Za-z0-9\\s.,!?'-]*$", message = "Description can only contain letters, numbers, spaces, and basic punctuation")
        String description,

        @Size(max = 500, message = "Image URL cannot exceed 500 characters")
        @Pattern(regexp = "^(https?://.+)?$", message = "Image URL must be a valid HTTP or HTTPS URL")
        String imageUrl,

        @NotNull(message = "Duration is required")
        @Positive(message = "Duration must be greater than 0")
        @Max(value = 30, message = "Duration cannot exceed 30 days")
        Integer duration,

        @Size(max = 10, message = "Cannot list more than 10 destinations")
        List<@NotBlank(message = "Destination cannot be blank") @Size(max = 30, message = "Destination name cannot exceed 30 characters") String> destinations,

        @NotNull (message = "Start date is required")
        LocalDate startDate,

        @NotNull (message = "End date is required")
        LocalDate endDate,

        @NotBlank (message = "accommodation is required")
        @Size(max = 30, message = "Accommodation cannot exceed 30 characters")
        @Pattern (regexp = "^[A-Za-z0-9\\s.,'-]*$", message = "Accommodation can only contain letters, numbers, spaces, and basic punctuation")
        String accommodation,

        @NotBlank (message = "transportation is required")
        @Size(max = 20, message = "Transportation cannot exceed 20 characters")
        @Pattern (regexp = "^[A-Za-z0-9\\s.,'-]*$", message = "Transportation can only contain letters, numbers, spaces, and basic punctuation")
        String transportation,

        @NotBlank(message = "Payment method is required")
        @Pattern(regexp = "Stripe|PayPal", message = "Payment method must be either 'Stripe' or 'PayPal'")
        String paymentMethod,

        @Size(max = 10, message = "Itinerary cannot contain more than 10 items")
        List<@Valid ItineraryRequest> itinerary) {
}
