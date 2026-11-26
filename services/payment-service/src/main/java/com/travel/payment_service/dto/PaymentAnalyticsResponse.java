package com.travel.payment_service.dto;

import java.math.BigDecimal;
import java.util.List;

public record PaymentAnalyticsResponse(
        int months,
        String currency,
        BigDecimal totalIncome,
        List<MonthlyIncome> monthlyIncome,
        List<TravelIncome> topTravels,
        List<ManagerIncome> managerIncome) {

    public record MonthlyIncome(String month, BigDecimal income, long payments) {
    }

    public record TravelIncome(Long travelId, String title, BigDecimal income, long payments) {
    }

    public record ManagerIncome(Long managerId, BigDecimal income, long payments) {
    }
}
