package com.travel.travel_service.controller;

import com.travel.travel_service.model.TravelEntity;
import com.travel.travel_service.dto.TravelResponse;
import com.travel.travel_service.repository.TravelRepository;
import com.travel.travel_service.services.TravelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/travels")
public class travelController {

    private final TravelRepository travelRepository;
    private final TravelService travelService;

    public travelController(TravelRepository travelRepository, TravelService travelService) {
        this.travelRepository = travelRepository;
        this.travelService = travelService;
    }

    @GetMapping
    public List<TravelResponse> listAll() {
        return travelService.getAllTravels();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TravelResponse> getById(@PathVariable Long id) {
        return travelService.getTravel(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public TravelEntity create(@RequestBody TravelEntity travel) {
        return travelRepository.save(travel);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TravelEntity> update(@PathVariable Long id, @RequestBody TravelEntity payload) {
        return travelRepository.findById(id)
                .map(existing -> {
                    existing.setTitle(payload.getTitle());
                    existing.setCity(payload.getCity());
                    existing.setCountry(payload.getCountry());
                    existing.setDescription(payload.getDescription());
                    existing.setImageUrl(payload.getImageUrl());
                    existing.setDuration(payload.getDuration());
                    return ResponseEntity.ok(travelRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!travelRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        travelRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
