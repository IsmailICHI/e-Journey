package com.travel.travel_service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.travel.travel_service.models.TravelEntity;

public interface TravelRepository extends JpaRepository<TravelEntity, Long> {
    List<TravelEntity> findAllByOrderByTravelIdAsc();

    Page<TravelEntity> findAllByOrderByTravelIdAsc(Pageable pageable);
}
