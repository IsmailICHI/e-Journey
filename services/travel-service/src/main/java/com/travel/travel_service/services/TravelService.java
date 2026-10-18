package com.travel.travel_service.services;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.travel.travel_service.dto.ItineraryResponse;
import com.travel.travel_service.dto.ItineraryRequest;
import com.travel.travel_service.dto.TravelPageResponse;
import com.travel.travel_service.dto.TravelRequest;
import com.travel.travel_service.dto.TravelResponse;
import com.travel.travel_service.repository.TravelRepository;
import com.travel.travel_service.exceptions.BadRequestException;
import com.travel.travel_service.models.ItineraryEntity;
import com.travel.travel_service.models.TravelEntity;

import org.springframework.transaction.annotation.Transactional;

@Service
public class TravelService {
        private static final int ADMIN_PAGE_SIZE = 5;

        private final TravelRepository travelRepository;

        public TravelService(TravelRepository travelRepository) {
                this.travelRepository = travelRepository;
        }

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

        @Transactional(readOnly = true)
        public TravelPageResponse getTravelPage(int page) {
                Page<TravelEntity> travels = travelRepository.findAllByOrderByTravelIdAsc(
                                PageRequest.of(Math.max(0, page), ADMIN_PAGE_SIZE, Sort.unsorted()));

                List<TravelResponse> content = travels.getContent().stream()
                                .map(this::toResponse)
                                .toList();
                return new TravelPageResponse(
                                content,
                                travels.getNumber(),
                                travels.getSize(),
                                travels.getTotalElements(),
                                travels.getTotalPages(),
                                travels.isLast());
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
                                List.copyOf(travel.getDestinations()),
                                travel.getStartDate(),
                                travel.getEndDate(),
                                travel.getAccommodation(),
                                travel.getTransportation(),
                                travel.getPaymentMethod(),
                                itinerary);
        }

        @Transactional
        public TravelResponse createTravel(TravelRequest request) {
                TravelEntity travel = new TravelEntity();
                applyRequest(travel, request);
                return toResponse(travelRepository.save(travel));
        }

        @Transactional
        public Optional<TravelResponse> updateTravel(Long id, TravelRequest request) {
                return travelRepository.findById(id).map(travel -> {
                        applyRequest(travel, request);
                        return toResponse(travelRepository.save(travel));
                });
        }

        @Transactional
        public boolean deleteTravel(Long id) {
                if (!travelRepository.existsById(id)) {
                        return false;
                }
                travelRepository.deleteById(id);
                return true;
        }

        private void applyRequest(TravelEntity travel, TravelRequest request) {
                if (request.startDate() != null && request.endDate() != null
                                && request.endDate().isBefore(request.startDate())) {
                        throw new BadRequestException("End date (" + request.endDate()
                                        + ") must be on or after start date (" + request.startDate() + ")");
                }

                if (request.itinerary() != null) {
                        java.util.Set<Integer> seenDays = new java.util.HashSet<>();
                        for (ItineraryRequest item : request.itinerary()) {
                                if (item.dayNumber() != null && !seenDays.add(item.dayNumber())) {
                                        throw new BadRequestException(
                                                        "Duplicate itinerary day number: Day " + item.dayNumber());
                                }
                        }
                }

                travel.setTitle(request.title() != null ? request.title().trim() : null);
                travel.setCity(request.city() != null ? request.city().trim() : null);
                travel.setCountry(request.country() != null ? request.country().trim() : null);
                travel.setDescription(request.description() != null ? request.description().trim() : null);
                travel.setImageUrl(request.imageUrl() != null ? request.imageUrl().trim() : null);
                travel.setDuration(request.duration());
                travel.setDestinations(request.destinations() == null
                                ? new ArrayList<>()
                                : new ArrayList<>(request.destinations().stream()
                                                .map(String::trim)
                                                .filter(s -> !s.isEmpty())
                                                .toList()));
                travel.setStartDate(request.startDate());
                travel.setEndDate(request.endDate());
                travel.setAccommodation(request.accommodation() != null ? request.accommodation().trim() : null);
                travel.setTransportation(request.transportation() != null ? request.transportation().trim() : null);
                travel.setPaymentMethod(request.paymentMethod());
                travel.getItinerary().clear();

                if (request.itinerary() != null) {
                        request.itinerary().stream()
                                        .map(this::toEntity)
                                        .forEach(item -> {
                                                item.setTravel(travel);
                                                travel.getItinerary().add(item);
                                        });
                }
        }

        private ItineraryEntity toEntity(ItineraryRequest request) {
                return ItineraryEntity.builder()
                                .dayNumber(request.dayNumber())
                                .title(request.title())
                                .description(request.description())
                                .activities(request.activities() == null
                                                ? new ArrayList<>()
                                                : new ArrayList<>(request.activities()))
                                .build();
        }
}
