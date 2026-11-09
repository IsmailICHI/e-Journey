package com.travel.booking_service.events;

import java.time.Instant;

public record BookingCreatedEvent(
        String eventId,
        Long bookingId,
        Long userId,
        Long travelId,
        Instant createdAt) {
}
