package com.travel.booking_service.dto;

import com.travel.booking_service.model.TravelerReport.TargetType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotNull TargetType targetType,
        @NotNull @Positive Long targetId,
        @NotBlank @Size(max = 120) String reason,
        @NotBlank @Size(max = 2000) String details) {
}
