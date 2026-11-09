package com.travel.payment_service.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.travel.payment_service.client.BookingDetailsClient;
import com.travel.payment_service.dto.PaymentAnalyticsResponse;
import com.travel.payment_service.dto.PaymentOrderResponse;
import com.travel.payment_service.model.Payment;
import com.travel.payment_service.model.PaymentStatus;
import com.travel.payment_service.provider.PayPalClient;
import com.travel.payment_service.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingDetailsClient bookingDetailsClient;
    private final PayPalClient payPalClient;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingDetailsClient bookingDetailsClient,
            PayPalClient payPalClient) {
        this.paymentRepository = paymentRepository;
        this.bookingDetailsClient = bookingDetailsClient;
        this.payPalClient = payPalClient;
    }

    @Transactional
    public PaymentOrderResponse createOrder(Long userId, Long bookingId, String authorization) {
        BookingDetailsClient.BookingSnapshot booking = bookingDetailsClient.getBooking(bookingId, authorization);
        if (!booking.userId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This booking belongs to another traveler");
        }
        return paymentRepository.findByBookingId(bookingId)
                .map(PaymentOrderResponse::from)
                .orElseGet(() -> createNewOrder(booking));
    }

    private PaymentOrderResponse createNewOrder(BookingDetailsClient.BookingSnapshot booking) {
        Map<String, Object> order = payPalClient.createOrder(booking.id(), booking.priceMad(), "MAD");
        String orderId = (String) order.get("id");
        String approvalUrl = approvalUrl(order);
        Payment payment = new Payment(
                booking.id(),
                booking.userId(),
                booking.travelId(),
                booking.managerId(),
                booking.travelTitle(),
                booking.priceMad(),
                "MAD",
                orderId,
                approvalUrl);
        return PaymentOrderResponse.from(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentOrderResponse capture(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (!payment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This payment belongs to another traveler");
        }
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return PaymentOrderResponse.from(payment);
        }

        Map<String, Object> capture = payPalClient.captureOrder(payment.getProviderOrderId());
        if (!"COMPLETED".equals(capture.get("status")) || !captureMatches(capture, payment)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "PayPal did not confirm the expected payment amount and currency");
        }
        payment.complete();
        return PaymentOrderResponse.from(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public List<PaymentOrderResponse> getPaymentHistory() {
        return paymentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(PaymentOrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentAnalyticsResponse getAnalytics(int requestedMonths) {
        int months = Math.max(1, Math.min(requestedMonths, 12));
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        Instant from = currentMonth.minusMonths(months - 1L)
                .atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Payment> payments = paymentRepository
                .findByStatusAndCompletedAtGreaterThanEqualOrderByCompletedAtAsc(PaymentStatus.COMPLETED, from);
        Map<YearMonth, IncomeTotals> byMonth = new HashMap<>();
        Map<Long, IncomeTotals> byTravel = new HashMap<>();
        Map<Long, IncomeTotals> byManager = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Payment payment : payments) {
            YearMonth month = YearMonth.from(payment.getCompletedAt().atZone(ZoneOffset.UTC));
            if (month.isAfter(currentMonth)) {
                continue;
            }
            total = total.add(payment.getAmount());
            byMonth.computeIfAbsent(month, ignored -> new IncomeTotals()).add(payment.getAmount());
            byTravel.computeIfAbsent(payment.getTravelId(), ignored -> new IncomeTotals(payment.getTravelTitle()))
                    .add(payment.getAmount());
            if (payment.getManagerId() != null) {
                byManager.computeIfAbsent(payment.getManagerId(), ignored -> new IncomeTotals())
                        .add(payment.getAmount());
            }
        }

        List<PaymentAnalyticsResponse.MonthlyIncome> monthly = new ArrayList<>();
        for (int offset = months - 1; offset >= 0; offset--) {
            YearMonth month = currentMonth.minusMonths(offset);
            IncomeTotals income = byMonth.getOrDefault(month, new IncomeTotals());
            monthly.add(new PaymentAnalyticsResponse.MonthlyIncome(
                    month.toString(), income.amount, income.count));
        }
        List<PaymentAnalyticsResponse.TravelIncome> topTravels = byTravel.entrySet().stream()
                .map(entry -> new PaymentAnalyticsResponse.TravelIncome(
                        entry.getKey(), entry.getValue().label, entry.getValue().amount, entry.getValue().count))
                .sorted(Comparator.comparing(PaymentAnalyticsResponse.TravelIncome::income).reversed())
                .toList();
        List<PaymentAnalyticsResponse.ManagerIncome> managers = byManager.entrySet().stream()
                .map(entry -> new PaymentAnalyticsResponse.ManagerIncome(
                        entry.getKey(), entry.getValue().amount, entry.getValue().count))
                .sorted(Comparator.comparing(PaymentAnalyticsResponse.ManagerIncome::income).reversed())
                .toList();
        return new PaymentAnalyticsResponse(months, "MAD", total, monthly, topTravels, managers);
    }

    private String approvalUrl(Map<String, Object> order) {
        Object linksValue = order.get("links");
        if (linksValue instanceof Iterable<?> links) {
            for (Object linkValue : links) {
                if (linkValue instanceof Map<?, ?> link
                        && Objects.equals(link.get("rel"), "approve")
                        && link.get("href") instanceof String href) {
                    return href;
                }
            }
        }
        throw new IllegalStateException("PayPal order did not include an approval link");
    }

    private boolean captureMatches(Map<String, Object> capture, Payment payment) {
        Object unitsValue = capture.get("purchase_units");
        if (!(unitsValue instanceof List<?> units) || units.isEmpty()
                || !(units.get(0) instanceof Map<?, ?> unit)
                || !(unit.get("payments") instanceof Map<?, ?> payments)
                || !(payments.get("captures") instanceof List<?> captures)
                || captures.isEmpty()
                || !(captures.get(0) instanceof Map<?, ?> captureDetails)
                || !(captureDetails.get("amount") instanceof Map<?, ?> amount)) {
            return false;
        }
        return Objects.equals(amount.get("currency_code"), payment.getCurrency())
                && new BigDecimal(String.valueOf(amount.get("value"))).compareTo(payment.getAmount()) == 0;
    }

    private static final class IncomeTotals {
        private final String label;
        private BigDecimal amount = BigDecimal.ZERO;
        private long count;

        private IncomeTotals() {
            this(null);
        }

        private IncomeTotals(String label) {
            this.label = label;
        }

        private void add(BigDecimal value) {
            amount = amount.add(value);
            count++;
        }
    }
}
