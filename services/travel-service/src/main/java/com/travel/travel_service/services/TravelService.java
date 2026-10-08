package com.travel.travel_service.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.travel.travel_service.dto.ItineraryResponse;
import com.travel.travel_service.dto.TravelResponse;
import com.travel.travel_service.model.TravelEntity;
import com.travel.travel_service.repository.TravelRepository;
import org.springframework.transaction.annotation.Transactional;

import lombok.Data;

@Service
@Data
public class TravelService {

        private final TravelRepository travelRepository;

        @Transactional(readOnly = true)
        public Optional<TravelResponse> getTravel(Long id) {
                return travelRepository.findById(id).map(this::toResponse);
        }

        @Transactional(readOnly = true)
        public List<TravelResponse> getAllTravels() {

                return travelRepository.findAll()
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        private TravelResponse toResponse(TravelEntity travel) {
                List<ItineraryResponse> itinerary = travel.getItinerary()
                                .stream()
                                .map(item -> new ItineraryResponse(
                                                item.getId(),
                                                item.getDayNumber(),
                                                item.getTitle(),
                                                item.getDescription(),
                                                List.copyOf(item.getActivities())))
                                .toList();

                return new TravelResponse(
                                travel.getTravelId(),
                                travel.getTitle(),
                                travel.getCity(),
                                travel.getCountry(),
                                travel.getDescription(),
                                travel.getImageUrl(),
                                travel.getDuration(),
                                itinerary);
        }
}
