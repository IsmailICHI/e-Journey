package com.travel.booking_service.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.booking_service.dto.BookingResponse;
import com.travel.booking_service.dto.CreateBookingRequest;
import com.travel.booking_service.events.BookingCreatedEvent;
import com.travel.booking_service.model.Booking;
import com.travel.booking_service.model.BookingEventOutbox;
import com.travel.booking_service.repository.BookingEventOutboxRepository;
import com.travel.booking_service.repository.BookingRepository;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingEventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public BookingService(
            BookingRepository bookingRepository,
            BookingEventOutboxRepository outboxRepository,
            ObjectMapper objectMapper) {
        this.bookingRepository = bookingRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BookingResponse createBooking(Long userId, CreateBookingRequest request) {
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setTravelId(request.travelId());
        booking = bookingRepository.save(booking);

        BookingCreatedEvent event = new BookingCreatedEvent(
                UUID.randomUUID().toString(),
                booking.getId(),
                booking.getUserId(),
                booking.getTravelId(),
                booking.getCreatedAt());
        try {
            outboxRepository.save(new BookingEventOutbox(objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize booking event", exception);
        }

        return BookingResponse.from(booking);
    }
}
