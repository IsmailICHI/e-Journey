package com.travel.booking_service.dto;

import java.time.Instant;

import com.travel.booking_service.model.TravelerFeedback;

public record FeedbackResponse(
        Long id,
        Long bookingId,
        Long travelerId,
        Long travelId,
        Long managerId,
        Integer rating,
        String comment,
        Instant createdAt) {

    public static FeedbackResponse from(TravelerFeedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getBookingId(),
                feedback.getTravelerId(),
                feedback.getTravelId(),
                feedback.getManagerId(),
                feedback.getRating(),
                feedback.getComment(),
                feedback.getCreatedAt());
    }
}
