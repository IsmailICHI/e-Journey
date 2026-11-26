package com.travel.booking_service.dto;

import java.time.Instant;

import com.travel.booking_service.model.TravelerReport;

public record ReportResponse(
        Long id,
        Long reporterId,
        String targetType,
        Long targetId,
        String reason,
        String details,
        String status,
        Instant createdAt,
        Instant closedAt) {

    public static ReportResponse from(TravelerReport report) {
        return new ReportResponse(
                report.getId(),
                report.getReporterId(),
                report.getTargetType().name(),
                report.getTargetId(),
                report.getReason(),
                report.getDetails(),
                report.getStatus().name(),
                report.getCreatedAt(),
                report.getClosedAt());
    }
}
