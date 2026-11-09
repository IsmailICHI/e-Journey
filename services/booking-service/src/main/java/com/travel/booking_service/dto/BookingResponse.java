package com.travel.booking_service.dto;

import java.time.Instant;
import java.math.BigDecimal;

import com.travel.booking_service.model.Booking;

public record BookingResponse(
        Long id,
        Long travelId,
        Long userId,
        String travelTitle,
        BigDecimal priceMad,
        Long managerId,
        String status,
        boolean feedbackSubmitted,
        Instant createdAt) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getTravelId(),
                booking.getUserId(),
                booking.getTravelTitle(),
                booking.getPriceMad(),
                booking.getManagerId(),
                booking.getStatus().name(),
                booking.isFeedbackSubmitted(),
                booking.getCreatedAt());
    }
}
