package com.travel.travel_service.dto;

import java.util.List;

public record TravelPageResponse(
        List<TravelResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {
}
