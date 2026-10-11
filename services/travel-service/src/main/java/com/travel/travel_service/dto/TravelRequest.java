package com.travel.travel_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;

public record TravelRequest(
        @NotBlank String title,
        @NotBlank String city,
        @NotBlank String country,
        String description,
        String imageUrl,
        @NotNull @Positive Integer duration,
        List<String> destinations,
        LocalDate startDate,
        LocalDate endDate,
        String accommodation,
        String transportation,
        @NotBlank @Pattern(regexp = "Stripe|PayPal") String paymentMethod,
        List<@Valid ItineraryRequest> itinerary) {
}
