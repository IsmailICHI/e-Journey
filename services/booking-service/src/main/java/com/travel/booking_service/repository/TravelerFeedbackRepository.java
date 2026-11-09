package com.travel.booking_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.travel.booking_service.model.TravelerFeedback;

public interface TravelerFeedbackRepository extends JpaRepository<TravelerFeedback, Long> {

    boolean existsByBookingId(Long bookingId);

    List<TravelerFeedback> findAllByOrderByCreatedAtDesc();
}
