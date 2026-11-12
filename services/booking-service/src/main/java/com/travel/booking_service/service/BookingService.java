package com.travel.booking_service.service;

import java.util.UUID;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.booking_service.dto.BookingResponse;
import com.travel.booking_service.dto.CreateBookingRequest;
import com.travel.booking_service.events.BookingCreatedEvent;
import com.travel.booking_service.model.Booking;
import com.travel.booking_service.model.BookingEventOutbox;
import com.travel.booking_service.model.BookingStatus;
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
        if (travel.departureDate() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "This journey has no departure date, so its booking cutoff cannot be verified");
        }
        if (!travel.departureDate().isAfter(LocalDate.now(ZoneOffset.UTC).plusDays(3))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Bookings can only be created more than 3 days before departure");
        }
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setTravelId(travel.id());
        booking.setTravelTitle(travel.title());
        booking.setPriceMad(travel.priceMad());
        booking.setManagerId(travel.managerId());
        booking.setPaymentMethod(travel.paymentMethod());
        booking.setDepartureDate(travel.departureDate());
        booking.setCity(travel.city());
        booking.setCountry(travel.country());
        booking.setDuration(travel.duration());
        booking.setDestinations(travel.destinations());
        booking = bookingRepository.save(booking);

        enqueueGraphEvent(booking, "BOOKING_CREATED", null);

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
    public java.util.List<BookingResponse> getBookingsForManager(Long managerId) {
        return bookingRepository.findByManagerIdOrderByCreatedAtDesc(managerId).stream()
                .map(BookingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getManagerBookingCount(Long managerId) {
        return bookingRepository.countByManagerId(managerId);
    }

    @Transactional(readOnly = true)
    public Booking getBookingForManager(Long bookingId, Long managerId) {
        return bookingRepository.findById(bookingId)
                .filter(booking -> managerId.equals(booking.getManagerId()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Booking not found"));
    }

    @Transactional
    public void cancelPendingBookingForManager(Long bookingId, Long managerId) {
        Booking booking = getBookingForManager(bookingId, managerId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Only bookings awaiting payment can be unsubscribed");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        enqueueGraphEvent(booking, "BOOKING_STATUS_UPDATED", null);
    }

    @Transactional
    public void cancelPendingBookingForTraveler(Long bookingId, Long userId) {
        Booking booking = getOwnedBooking(bookingId, userId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Only bookings awaiting payment can be cancelled; paid bookings require a refund flow");
        }
        if (booking.getDepartureDate() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "This journey has no departure date, so its cancellation cutoff cannot be verified");
        }
        if (!booking.getDepartureDate().isAfter(LocalDate.now(ZoneOffset.UTC).plusDays(3))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Bookings can only be cancelled more than 3 days before departure");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        enqueueGraphEvent(booking, "BOOKING_STATUS_UPDATED", null);
    }

    @Transactional(readOnly = true)
    public com.travel.booking_service.dto.TravelerStatisticsResponse getTravelerStatistics(
            Long userId,
            long reportsFiled) {
        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long pastTrips = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED
                        && booking.getDepartureDate() != null
                        && booking.getDepartureDate().isBefore(LocalDate.now(ZoneOffset.UTC)))
                .count();
        long cancellations = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CANCELLED)
                .count();
        Map<String, Long> paymentMethods = new HashMap<>();
        bookings.stream()
                .map(Booking::getPaymentMethod)
                .filter(method -> method != null && !method.isBlank())
                .map(method -> method.trim().toUpperCase(Locale.ROOT))
                .forEach(method -> paymentMethods.merge(method, 1L, Long::sum));
        String preferredMethod = paymentMethods.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        return new com.travel.booking_service.dto.TravelerStatisticsResponse(
                pastTrips, reportsFiled, cancellations, preferredMethod);
    }

    private void enqueueGraphEvent(Booking booking, String eventType, Integer rating) {
        BookingCreatedEvent event = new BookingCreatedEvent(
                UUID.randomUUID().toString(),
                booking.getId(),
                booking.getUserId(),
                booking.getTravelId(),
                booking.getCreatedAt(),
                eventType,
                booking.getCity(),
                booking.getCountry(),
                booking.getDuration(),
                booking.getPaymentMethod(),
                booking.getDestinations(),
                booking.getDepartureDate(),
                rating,
                booking.getStatus().name());
        try {
            outboxRepository.save(new BookingEventOutbox(objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize booking graph event", exception);
        }
    }

    @Transactional
    public void recordFeedback(Booking booking, Integer rating) {
        enqueueGraphEvent(booking, "FEEDBACK_SUBMITTED", rating);
    }

    @Transactional
    public void confirmPayment(Long bookingId, Long userId) {
        Booking booking = getOwnedBooking(bookingId, userId);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "This booking was cancelled");
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return;
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "This booking is not awaiting payment");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        enqueueGraphEvent(booking, "BOOKING_STATUS_UPDATED", null);
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

    @Transactional(readOnly = true)
    public Booking getActiveBookingForUser(Long bookingId, Long userId) {
        Booking booking = getOwnedBooking(bookingId, userId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "This booking is not awaiting payment");
        }
        return booking;
    }
}
