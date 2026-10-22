package com.travel.travel_service.dto;

import java.util.List;
import java.time.LocalDate;

public record TravelResponse(
        Long travelId,
        String title,
        String city,
        String country,
        String description,
        String imageUrl,
        Integer duration,
        List<String> destinations,
        LocalDate startDate,
        LocalDate endDate,
        String accommodation,
        String transportation,
        String paymentMethod,
        List<ItineraryResponse> itinerary
) {
}