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
import com.travel.payment_service.provider.StripeClient;
import com.travel.payment_service.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingDetailsClient bookingDetailsClient;
    private final PayPalClient payPalClient;
    private final StripeClient stripeClient;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingDetailsClient bookingDetailsClient,
            PayPalClient payPalClient,
            StripeClient stripeClient) {
        this.paymentRepository = paymentRepository;
        this.bookingDetailsClient = bookingDetailsClient;
        this.payPalClient = payPalClient;
        this.stripeClient = stripeClient;
    }

    @Transactional
    public PaymentOrderResponse createOrder(Long userId, Long bookingId, String authorization) {
        BookingDetailsClient.BookingSnapshot booking = bookingDetailsClient.getBooking(bookingId, authorization);
        if (!booking.userId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This booking belongs to another traveler");
        }
        requirePendingBooking(booking);
        return paymentRepository.findByBookingId(bookingId)
                .map(PaymentOrderResponse::from)
                .orElseGet(() -> createNewOrder(booking));
    }

    private PaymentOrderResponse createNewOrder(BookingDetailsClient.BookingSnapshot booking) {
        String provider = normalizeProvider(booking.paymentMethod());
        Map<String, Object> order;
        String approvalUrl;
        if ("PAYPAL".equals(provider)) {
            order = payPalClient.createOrder(booking.id(), booking.priceMad(), "MAD");
            approvalUrl = approvalUrl(order);
        } else {
            order = stripeClient.createCheckoutSession(
                    booking.id(), booking.travelTitle(), booking.priceMad(), "MAD");
            approvalUrl = (String) order.get("url");
        }
        String orderId = (String) order.get("id");
        Payment payment = new Payment(
                booking.id(),
                booking.userId(),
                booking.travelId(),
                booking.managerId(),
                booking.travelTitle(),
                booking.priceMad(),
                "MAD",
                provider,
                orderId,
                approvalUrl);
        return PaymentOrderResponse.from(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentOrderResponse capture(Long userId, Long paymentId, String authorization) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (!payment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This payment belongs to another traveler");
        }
        if (!"PAYPAL".equals(payment.getProvider())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Use Stripe checkout verification for this payment");
        }
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return PaymentOrderResponse.from(payment);
        }

        requirePendingBooking(bookingDetailsClient.getBooking(payment.getBookingId(), authorization));
        Map<String, Object> capture = payPalClient.captureOrder(payment.getProviderOrderId());
        if (!"COMPLETED".equals(capture.get("status")) || !captureMatches(capture, payment)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "PayPal did not confirm the expected payment amount and currency");
        }
        bookingDetailsClient.confirmPayment(payment.getBookingId(), authorization);
        payment.complete();
        return PaymentOrderResponse.from(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentOrderResponse completeStripeSession(Long userId, String sessionId, String authorization) {
        Payment payment = paymentRepository.findByProviderOrderIdAndProvider(sessionId, "STRIPE")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Stripe checkout session not found"));
        if (!payment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This payment belongs to another traveler");
        }
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return PaymentOrderResponse.from(payment);
        }

        requirePendingBooking(bookingDetailsClient.getBooking(payment.getBookingId(), authorization));
        Map<String, Object> session = stripeClient.getCheckoutSession(sessionId);
        if (!stripeSessionMatches(session, payment)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Stripe did not confirm the expected paid checkout session");
        }
        bookingDetailsClient.confirmPayment(payment.getBookingId(), authorization);
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
        return getAnalytics(null, requestedMonths);
    }

    @Transactional(readOnly = true)
    public PaymentAnalyticsResponse getManagerAnalytics(Long managerId, int requestedMonths) {
        return getAnalytics(managerId, requestedMonths);
    }

    private PaymentAnalyticsResponse getAnalytics(Long managerId, int requestedMonths) {
        int months = Math.max(1, Math.min(requestedMonths, 12));
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        Instant from = currentMonth.minusMonths(months - 1L)
                .atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Payment> payments = managerId == null
                ? paymentRepository.findByStatusAndCompletedAtGreaterThanEqualOrderByCompletedAtAsc(
                        PaymentStatus.COMPLETED, from)
                : paymentRepository.findByManagerIdAndStatusAndCompletedAtGreaterThanEqualOrderByCompletedAtAsc(
                        managerId, PaymentStatus.COMPLETED, from);
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

    private void requirePendingBooking(BookingDetailsClient.BookingSnapshot booking) {
        if (!"PENDING_PAYMENT".equals(booking.status())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This booking is not awaiting payment");
        }
    }

    private String normalizeProvider(String paymentMethod) {
        String provider = paymentMethod == null
                ? "PAYPAL"
                : paymentMethod.trim().toUpperCase(java.util.Locale.ROOT);
        if (!"PAYPAL".equals(provider) && !"STRIPE".equals(provider)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported travel payment method");
        }
        return provider;
    }

    private boolean stripeSessionMatches(Map<String, Object> session, Payment payment) {
        Object amount = session.get("amount_total");
        return "complete".equals(session.get("status"))
                && "paid".equals(session.get("payment_status"))
                && payment.getBookingId().toString().equals(String.valueOf(session.get("client_reference_id")))
                && amount instanceof Number total
                && new BigDecimal(total.toString()).compareTo(payment.getAmount().movePointRight(2)) == 0
                && payment.getCurrency().equalsIgnoreCase(String.valueOf(session.get("currency")));
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
