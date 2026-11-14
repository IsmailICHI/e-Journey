package com.travel.payment_service.dto;

import com.travel.payment_service.model.Payment;

public record PaymentOrderResponse(
        Long paymentId,
        Long bookingId,
        Long travelId,
        Long managerId,
        java.math.BigDecimal amount,
        java.math.BigDecimal amountMad,
        java.math.BigDecimal madPerUsdRate,
        String currency,
        String providerOrderId,
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
                payment.getAmountMad(),
                payment.getMadPerUsdRate(),
                payment.getCurrency(),
                payment.getProviderOrderId(),
                payment.getProvider(),
                payment.getStatus().name(),
                payment.getApprovalUrl());
    }
}
