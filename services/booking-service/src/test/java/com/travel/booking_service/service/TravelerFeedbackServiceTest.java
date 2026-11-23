package com.travel.booking_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneOffset;

import com.travel.booking_service.dto.CreateFeedbackRequest;
import com.travel.booking_service.model.Booking;
import com.travel.booking_service.model.BookingStatus;
import com.travel.booking_service.repository.TravelerFeedbackRepository;
import com.travel.booking_service.repository.TravelerReportRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class TravelerFeedbackServiceTest {

    private static final String AUTHORIZATION = "Bearer test-token";

    @Mock
    private BookingService bookingService;

    @Mock
    private TravelerFeedbackRepository feedbackRepository;

    @Mock
    private TravelerReportRepository reportRepository;

    private TravelerFeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        feedbackService = new TravelerFeedbackService(bookingService, feedbackRepository, reportRepository);
    }

    @Test
    void acceptsFeedbackWhenCurrentTravelDateHasPassedDespiteStaleBookingDate() {
        Booking booking = new Booking();
        booking.setUserId(7L);
        booking.setTravelId(11L);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setDepartureDate(LocalDate.now(ZoneOffset.UTC).plusDays(7));
        when(bookingService.getOwnedBooking(22L, 7L)).thenReturn(booking);
        when(bookingService.getCurrentDepartureDate(booking, AUTHORIZATION))
                .thenReturn(LocalDate.now(ZoneOffset.UTC).minusDays(3));
        when(feedbackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertNotNull(feedbackService.submitFeedback(
                7L, new CreateFeedbackRequest(22L, 5, "Great trip"), AUTHORIZATION));

        verify(feedbackRepository).save(any());
        verify(bookingService).recordFeedback(booking, 5);
    }

    @Test
    void rejectsFeedbackWhenCurrentTravelDateIsTodayOrInTheFuture() {
        Booking booking = new Booking();
        booking.setUserId(7L);
        booking.setTravelId(11L);
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingService.getOwnedBooking(22L, 7L)).thenReturn(booking);
        when(bookingService.getCurrentDepartureDate(booking, AUTHORIZATION))
                .thenReturn(LocalDate.now(ZoneOffset.UTC));

        ResponseStatusException exception = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> feedbackService.submitFeedback(
                        7L, new CreateFeedbackRequest(22L, 5, "Great trip"), AUTHORIZATION));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }
}
