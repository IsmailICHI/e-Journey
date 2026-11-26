package com.travel.booking_service.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

import com.travel.booking_service.model.Booking;

public record BookingResponse(
        Long id,
        Long travelId,
        Long userId,
        String travelTitle,
        String paymentMethod,
        LocalDate departureDate,
        BigDecimal priceMad,
        Long managerId,
        String status,
        boolean feedbackSubmitted,
        Instant createdAt) {

    public static BookingResponse from(Booking booking) {
        return from(booking, booking.getDepartureDate());
    }

    public static BookingResponse from(Booking booking, LocalDate departureDate) {
        return new BookingResponse(
                booking.getId(),
                booking.getTravelId(),
                booking.getUserId(),
                booking.getTravelTitle(),
                booking.getPaymentMethod(),
                departureDate,
                booking.getPriceMad(),
                booking.getManagerId(),
                booking.getStatus().name(),
                booking.isFeedbackSubmitted(),
                booking.getCreatedAt());
    }
}
