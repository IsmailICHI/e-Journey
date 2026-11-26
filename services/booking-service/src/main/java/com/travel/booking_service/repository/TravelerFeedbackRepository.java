package com.travel.booking_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.travel.booking_service.model.TravelerFeedback;

public interface TravelerFeedbackRepository extends JpaRepository<TravelerFeedback, Long> {

    boolean existsByBookingId(Long bookingId);

    List<TravelerFeedback> findAllByOrderByCreatedAtDesc();

    List<TravelerFeedback> findByManagerIdOrderByCreatedAtDesc(Long managerId);

    long countByManagerId(Long managerId);

    @org.springframework.data.jpa.repository.Query(
            "SELECT AVG(f.rating) FROM TravelerFeedback f WHERE f.managerId = :managerId")
    Double averageRatingByManagerId(@org.springframework.data.repository.query.Param("managerId") Long managerId);
}
