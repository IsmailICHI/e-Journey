package com.travel.payment_service.repository;

import java.util.Optional;
import java.util.List;
import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.travel.payment_service.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByProviderOrderId(String providerOrderId);

    Optional<Payment> findByBookingId(Long bookingId);

    List<Payment> findAllByOrderByCreatedAtDesc();

    List<Payment> findByStatusAndCompletedAtGreaterThanEqualOrderByCompletedAtAsc(
            com.travel.payment_service.model.PaymentStatus status, Instant from);
}
