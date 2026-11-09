package com.travel.booking_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import com.travel.booking_service.model.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findAllByOrderByCreatedAtDesc();

    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    List<Booking> findByManagerIdOrderByCreatedAtDesc(Long managerId);
}
