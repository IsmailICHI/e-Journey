package com.travel.travel_service.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.travel.travel_service.dto.TravelResponse;
import com.travel.travel_service.events.BookingCreatedEvent;
import com.travel.travel_service.repository.TravelGraphRepository;

@Service
public class BookingGraphService {

    private final TravelGraphRepository travelGraphRepository;

    public BookingGraphService(TravelGraphRepository travelGraphRepository) {
        this.travelGraphRepository = travelGraphRepository;
    }

    public void recordBooking(BookingCreatedEvent event) {
        switch (event.eventType()) {
            case "FEEDBACK_SUBMITTED" ->
                    travelGraphRepository.updateFeedback(event.bookingId(), event.rating());
            case "BOOKING_STATUS_UPDATED" ->
                    travelGraphRepository.updateBookingStatus(event.bookingId(), event.bookingStatus());
            default -> travelGraphRepository.upsertBooking(
                    event.userId(),
                    event.travelId(),
                    event.bookingId(),
                    event.createdAt().toString(),
                    event.bookingStatus(),
                    event.city(),
                    event.country(),
                    event.duration(),
                    event.paymentMethod());
        }
    }

    public void indexTravel(TravelResponse travel) {
        travelGraphRepository.upsertTravel(
                travel.travelId(),
                travel.city(),
                travel.country(),
                travel.duration(),
                travel.paymentMethod(),
                travel.startDate());
    }

    public void deleteTravel(Long travelId) {
        travelGraphRepository.markTravelUnavailable(travelId);
    }

    public List<Long> recommendTravels(Long userId, int limit) {
        return travelGraphRepository.recommendTravels(userId, Math.max(1, Math.min(limit, 20)));
    }
}
