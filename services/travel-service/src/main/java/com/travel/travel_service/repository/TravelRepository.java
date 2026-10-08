package com.travel.travel_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.travel.travel_service.model.TravelEntity;

public interface TravelRepository extends JpaRepository<TravelEntity, Long> {
    List<TravelEntity> findAllByOrderByTravelIdAsc();
}
