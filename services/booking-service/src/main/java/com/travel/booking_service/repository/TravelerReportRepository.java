package com.travel.booking_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.travel.booking_service.model.TravelerReport;

public interface TravelerReportRepository extends JpaRepository<TravelerReport, Long> {

    List<TravelerReport> findAllByOrderByCreatedAtDesc();
}
