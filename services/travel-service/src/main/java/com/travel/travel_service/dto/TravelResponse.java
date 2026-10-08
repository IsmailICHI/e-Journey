package com.travel.travel_service.dto;

import java.util.List;

public record TravelResponse(
        Long travelId,
        String title,
        String city,
        String country,
        String description,
        String imageUrl,
        Integer duration,
        List<ItineraryResponse> itinerary
) {
}