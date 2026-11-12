package com.travel.booking_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.travel.booking_service.dto.BookingResponse;
import com.travel.booking_service.dto.CreateBookingRequest;
import com.travel.booking_service.service.BookingService;
import com.travel.booking_service.model.Booking;
import com.travel.booking_service.dto.CreateFeedbackRequest;
import com.travel.booking_service.dto.CreateReportRequest;
import com.travel.booking_service.dto.FeedbackResponse;
import com.travel.booking_service.dto.ReportResponse;
import com.travel.booking_service.dto.TravelerStatisticsResponse;
import com.travel.booking_service.dto.ManagerPublicStatisticsResponse;
import com.travel.booking_service.model.TravelerReport;
import com.travel.booking_service.service.TravelerFeedbackService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final TravelerFeedbackService feedbackService;
    private final String paymentConfirmationSecret;

    public BookingController(
            BookingService bookingService,
            TravelerFeedbackService feedbackService,
            @Value("${app.booking.payment-confirmation-secret}") String paymentConfirmationSecret) {
        this.bookingService = bookingService;
        this.feedbackService = feedbackService;
        this.paymentConfirmationSecret = paymentConfirmationSecret;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateBookingRequest request) {
            return bookingService.createBooking(
                    userId(jwt),
                    request,
                    "Bearer " + jwt.getTokenValue());
    }

    @GetMapping("/me")
    public java.util.List<BookingResponse> myBookings(@AuthenticationPrincipal Jwt jwt) {
        return bookingService.getBookingsForUser(userId(jwt));
    }

    @GetMapping("/me/statistics")
    public TravelerStatisticsResponse myStatistics(@AuthenticationPrincipal Jwt jwt) {
        return bookingService.getTravelerStatistics(
                userId(jwt), feedbackService.getReportsForTraveler(userId(jwt)).size());
    }

    @GetMapping("/me/reports")
    public java.util.List<ReportResponse> myReports(@AuthenticationPrincipal Jwt jwt) {
        return feedbackService.getReportsForTraveler(userId(jwt));
    }

    @DeleteMapping("/me/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelMySubscription(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        bookingService.cancelPendingBookingForTraveler(id, userId(jwt));
    }

    @GetMapping("/{id}")
    public BookingResponse getBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        Booking booking = bookingService.getOwnedBooking(id, userId(jwt));
        return BookingResponse.from(booking);
    }

    @GetMapping("/admin/history")
    @PreAuthorize("hasRole('ADMIN')")
    public java.util.List<BookingResponse> allBookings() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/manager/history")
    @PreAuthorize("hasRole('TRAVEL_MANAGER')")
    public java.util.List<BookingResponse> managerBookings(@AuthenticationPrincipal Jwt jwt) {
        return bookingService.getBookingsForManager(userId(jwt));
    }

    @GetMapping("/manager/bookings/{id}")
    @PreAuthorize("hasRole('TRAVEL_MANAGER')")
    public BookingResponse managerBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        return BookingResponse.from(bookingService.getBookingForManager(id, userId(jwt)));
    }

    @DeleteMapping("/manager/bookings/{id}")
    @PreAuthorize("hasRole('TRAVEL_MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribeTraveler(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        bookingService.cancelPendingBookingForManager(id, userId(jwt));
    }

    @GetMapping("/manager/feedback")
    @PreAuthorize("hasRole('TRAVEL_MANAGER')")
    public java.util.List<FeedbackResponse> managerFeedback(@AuthenticationPrincipal Jwt jwt) {
        return feedbackService.getManagerFeedback(userId(jwt));
    }

    @GetMapping("/public/managers/{managerId}/statistics")
    public ManagerPublicStatisticsResponse managerPublicStatistics(@PathVariable Long managerId) {
        return feedbackService.getManagerPublicStatistics(managerId);
    }

    @GetMapping("/public/managers/{managerId}/feedback")
    public java.util.List<FeedbackResponse> managerPublicFeedback(@PathVariable Long managerId) {
        return feedbackService.getManagerFeedback(managerId);
    }

    @PostMapping("/payments/{id}/confirm")
    public void confirmPayment(
            @PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestHeader("X-Payment-Service-Token") String serviceToken,
            @AuthenticationPrincipal Jwt jwt) {
        if (paymentConfirmationSecret.isBlank()
                || !java.security.MessageDigest.isEqual(
                        paymentConfirmationSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        serviceToken.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Payment confirmation is restricted to the payment service");
        }
        bookingService.confirmPayment(id, userId(jwt));
    }

    @PostMapping("/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse submitFeedback(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateFeedbackRequest request) {
        return feedbackService.submitFeedback(userId(jwt), request);
    }

    @GetMapping("/admin/feedback")
    @PreAuthorize("hasRole('ADMIN')")
    public java.util.List<FeedbackResponse> allFeedback() {
        return feedbackService.getAllFeedback();
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse submitReport(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateReportRequest request) {
        return feedbackService.submitReport(userId(jwt), request);
    }

    @GetMapping("/admin/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public java.util.List<ReportResponse> allReports() {
        return feedbackService.getAllReports();
    }

    @org.springframework.web.bind.annotation.PatchMapping("/admin/reports/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ReportResponse updateReportStatus(
            @PathVariable Long id,
            @RequestBody ReportStatusRequest request) {
        if (request.status() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Report status is required");
        }
        return feedbackService.updateReportStatus(id, request.status());
    }

    public record ReportStatusRequest(TravelerReport.Status status) {
    }

    private Long userId(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("userId");
        if (!(userIdClaim instanceof Number userId) || userId.longValue() <= 0) {
            throw new IllegalStateException("Authenticated token does not contain a valid user ID");
        }
        return userId.longValue();
    }
}
