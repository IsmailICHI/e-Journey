package com.travel.booking_service.dto;

public record TravelerStatisticsResponse(
        long pastTrips,
        long reportsFiled,
        long cancellations,
        String preferredPaymentMethod) {
}
