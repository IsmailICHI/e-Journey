package com.travel.travel_service.events;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BookingCreatedEvent(
        String eventId,
        Long bookingId,
        Long userId,
        Long travelId,
        Instant createdAt,
        String eventType,
        String city,
        String country,
        Integer duration,
        String paymentMethod,
        List<String> destinations,
        LocalDate departureDate,
        Integer rating,
        String bookingStatus) {
}
