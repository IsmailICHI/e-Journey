package com.travel.travel_service.dto;

import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;

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
        BigDecimal priceMad,
        Long createdByUserId,
        List<ItineraryResponse> itinerary
) {
}