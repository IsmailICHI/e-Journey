package com.travel.booking_service.service;

import java.util.List;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.travel.booking_service.dto.CreateFeedbackRequest;
import com.travel.booking_service.dto.CreateReportRequest;
import com.travel.booking_service.dto.FeedbackResponse;
import com.travel.booking_service.dto.ReportResponse;
import com.travel.booking_service.model.Booking;
import com.travel.booking_service.model.BookingStatus;
import com.travel.booking_service.model.TravelerFeedback;
import com.travel.booking_service.model.TravelerReport;
import com.travel.booking_service.repository.TravelerFeedbackRepository;
import com.travel.booking_service.repository.TravelerReportRepository;

@Service
public class TravelerFeedbackService {

    private final BookingService bookingService;
    private final TravelerFeedbackRepository feedbackRepository;
    private final TravelerReportRepository reportRepository;

    public TravelerFeedbackService(
            BookingService bookingService,
            TravelerFeedbackRepository feedbackRepository,
            TravelerReportRepository reportRepository) {
        this.bookingService = bookingService;
        this.feedbackRepository = feedbackRepository;
        this.reportRepository = reportRepository;
    }

    @Transactional
    public FeedbackResponse submitFeedback(Long userId, CreateFeedbackRequest request) {
        Booking booking = bookingService.getOwnedBooking(request.bookingId(), userId);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Feedback can only be submitted for confirmed bookings");
        }
        if (booking.getDepartureDate() == null
                || !booking.getDepartureDate().isBefore(LocalDate.now(ZoneOffset.UTC))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Feedback can only be submitted after the journey has taken place");
        }
        if (feedbackRepository.existsByBookingId(booking.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Feedback was already submitted for this booking");
        }
        booking.setFeedbackSubmitted(true);
        FeedbackResponse feedback = FeedbackResponse.from(feedbackRepository.save(
                new TravelerFeedback(booking, request.rating(), request.comment().trim())));
        bookingService.recordFeedback(booking, request.rating());
        return feedback;
    }

    @Transactional
    public ReportResponse submitReport(Long userId, CreateReportRequest request) {
        if (request.targetType() == TravelerReport.TargetType.TRAVELER
                && request.targetId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot report your own account");
        }
        TravelerReport report = new TravelerReport(
                userId,
                request.targetType(),
                request.targetId(),
                request.reason().trim(),
                request.details().trim());
        return ReportResponse.from(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getAllFeedback() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(FeedbackResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getManagerFeedback(Long managerId) {
        return feedbackRepository.findByManagerIdOrderByCreatedAtDesc(managerId).stream()
                .map(FeedbackResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getReportsForTraveler(Long travelerId) {
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(travelerId).stream()
                .map(ReportResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public com.travel.booking_service.dto.ManagerPublicStatisticsResponse getManagerPublicStatistics(Long managerId) {
        Double averageRating = feedbackRepository.averageRatingByManagerId(managerId);
        return new com.travel.booking_service.dto.ManagerPublicStatisticsResponse(
                bookingService.getManagerBookingCount(managerId),
                feedbackRepository.countByManagerId(managerId),
                averageRating == null ? null : java.math.BigDecimal.valueOf(averageRating),
                reportRepository.countByTargetTypeAndTargetId(TravelerReport.TargetType.MANAGER, managerId));
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getAllReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ReportResponse::from)
                .toList();
    }

    @Transactional
    public ReportResponse updateReportStatus(Long reportId, TravelerReport.Status status) {
        TravelerReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        report.setStatus(status);
        return ReportResponse.from(report);
    }
}
