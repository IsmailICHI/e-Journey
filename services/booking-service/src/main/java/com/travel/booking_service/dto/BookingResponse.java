package com.travel.booking_service.dto;

import java.time.Instant;

import com.travel.booking_service.model.Booking;

public record BookingResponse(Long id, Long travelId, Long userId, String status, Instant createdAt) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getTravelId(),
                booking.getUserId(),
                booking.getStatus().name(),
                booking.getCreatedAt());
    }
}
