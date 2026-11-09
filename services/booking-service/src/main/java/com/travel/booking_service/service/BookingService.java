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
import com.travel.booking_service.client.TravelDetailsClient;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingEventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final TravelDetailsClient travelDetailsClient;

    public BookingService(
            BookingRepository bookingRepository,
            BookingEventOutboxRepository outboxRepository,
            ObjectMapper objectMapper,
            TravelDetailsClient travelDetailsClient) {
        this.bookingRepository = bookingRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.travelDetailsClient = travelDetailsClient;
    }

    @Transactional
    public BookingResponse createBooking(Long userId, CreateBookingRequest request, String authorization) {
        TravelDetailsClient.TravelSnapshot travel = travelDetailsClient.getTravel(request.travelId(), authorization);
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setTravelId(travel.id());
        booking.setTravelTitle(travel.title());
        booking.setPriceMad(travel.priceMad());
        booking.setManagerId(travel.managerId());
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

    @Transactional(readOnly = true)
    public java.util.List<BookingResponse> getBookingsForUser(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(BookingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<BookingResponse> getAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(BookingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Booking getOwnedBooking(Long bookingId, Long userId) {
        return bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Booking not found"));
    }

    @Transactional(readOnly = true)
    public Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Booking not found"));
    }
}
