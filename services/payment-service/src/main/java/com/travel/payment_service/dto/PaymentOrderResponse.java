package com.travel.payment_service.dto;

import com.travel.payment_service.model.Payment;

public record PaymentOrderResponse(
        Long paymentId,
        Long bookingId,
        Long travelId,
        Long managerId,
        java.math.BigDecimal amount,
        String currency,
        String provider,
        String status,
        String approvalUrl) {

    public static PaymentOrderResponse from(Payment payment) {
        return new PaymentOrderResponse(
                payment.getId(),
                payment.getBookingId(),
                payment.getTravelId(),
                payment.getManagerId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getProvider(),
                payment.getStatus().name(),
                payment.getApprovalUrl());
    }
}
