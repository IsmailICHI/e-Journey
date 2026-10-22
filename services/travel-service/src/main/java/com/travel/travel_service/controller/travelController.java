package com.travel.travel_service.controller;

import com.travel.travel_service.dto.TravelRequest;
import com.travel.travel_service.dto.TravelResponse;
import com.travel.travel_service.dto.TravelPageResponse;
import com.travel.travel_service.exceptions.ResourceNotFoundException;
import com.travel.travel_service.services.TravelService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/travels")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class travelController {

    private final TravelService travelService;

    public travelController(TravelService travelService) {
        this.travelService = travelService;
    }

    @GetMapping
    public TravelPageResponse listAll(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index must be 0 or greater") int page) {
        return travelService.getTravelPage(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TravelResponse> getById(
            @PathVariable @Positive(message = "Travel ID must be a positive number") Long id) {
        TravelResponse travel = travelService.getTravel(id)
                .orElseThrow(() -> new ResourceNotFoundException("Travel with ID " + id + " not found"));
        return ResponseEntity.ok(travel);
    }

    @PostMapping
    public ResponseEntity<TravelResponse> create(@Valid @RequestBody TravelRequest request) {
        TravelResponse created = travelService.createTravel(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/travels/" + created.travelId()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TravelResponse> update(
            @PathVariable @Positive(message = "Travel ID must be a positive number") Long id,
            @Valid @RequestBody TravelRequest request) {
        TravelResponse updated = travelService.updateTravel(id, request)
                .orElseThrow(() -> new ResourceNotFoundException("Travel with ID " + id + " not found"));
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive(message = "Travel ID must be a positive number") Long id) {
        if (!travelService.deleteTravel(id)) {
            throw new ResourceNotFoundException("Travel with ID " + id + " not found");
        }
        return ResponseEntity.noContent().build();
    }
}
