package com.travel.booking_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.travel.booking_service.client.TravelDetailsClient;
import com.travel.booking_service.dto.BookingResponse;
import com.travel.booking_service.dto.CreateBookingRequest;
import com.travel.booking_service.model.Booking;
import com.travel.booking_service.model.BookingStatus;
import com.travel.booking_service.repository.BookingEventOutboxRepository;
import com.travel.booking_service.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class BookingServiceTest {

    private static final String AUTHORIZATION = "Bearer test-token";

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingEventOutboxRepository outboxRepository;

    @Mock
    private TravelDetailsClient travelDetailsClient;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bookingService = new BookingService(
                bookingRepository, outboxRepository, new ObjectMapper().registerModule(new JavaTimeModule()),
                travelDetailsClient);
    }

    @Test
    void rejectsBookingWhenDepartureIsWithinThreeDays() {
        LocalDate departureDate = LocalDate.now(ZoneOffset.UTC).plusDays(3);
        when(travelDetailsClient.getTravel(1L, AUTHORIZATION)).thenReturn(travel(departureDate));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(2L, new CreateBookingRequest(1L), AUTHORIZATION));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void rejectsBookingWhenDepartureDateIsMissing() {
        when(travelDetailsClient.getTravel(1L, AUTHORIZATION)).thenReturn(travel(null));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(2L, new CreateBookingRequest(1L), AUTHORIZATION));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void allowsBookingWhenDepartureIsMoreThanThreeDaysAway() {
        when(travelDetailsClient.getTravel(1L, AUTHORIZATION))
                .thenReturn(travel(LocalDate.now(ZoneOffset.UTC).plusDays(7)));
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertNotNull(bookingService.createBooking(2L, new CreateBookingRequest(1L), AUTHORIZATION));

        verify(bookingRepository).save(any());
    }

    @Test
    void showsCurrentDepartureDateForConfirmedBookings() {
        Booking booking = new Booking();
        booking.setTravelId(1L);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setDepartureDate(LocalDate.now(ZoneOffset.UTC).plusDays(7));
        LocalDate currentDepartureDate = LocalDate.now(ZoneOffset.UTC).minusDays(3);
        when(bookingRepository.findByUserIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(booking));
        when(travelDetailsClient.getTravel(1L, AUTHORIZATION)).thenReturn(travel(currentDepartureDate));

        List<BookingResponse> bookings = bookingService.getBookingsForUser(2L, AUTHORIZATION);

        assertEquals(currentDepartureDate, bookings.get(0).departureDate());
    }

    private TravelDetailsClient.TravelSnapshot travel(LocalDate departureDate) {
        return new TravelDetailsClient.TravelSnapshot(
                1L, "Test journey", null, 3L, "PAYPAL", departureDate,
                "Test city", "Test country", 5, List.of("Test destination"));
    }
}
