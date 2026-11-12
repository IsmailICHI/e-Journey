package com.travel.booking_service.dto;

import java.math.BigDecimal;

public record ManagerPublicStatisticsResponse(
        long bookings,
        long feedbackCount,
        BigDecimal averageRating,
        long reportCount) {
}
