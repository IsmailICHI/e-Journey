package com.travel.travel_service.dto;

import java.util.List;

public record ItineraryResponse(
        Long id,
        Integer dayNumber,
        String title,
        String description,
        List<String> activities
) {
}