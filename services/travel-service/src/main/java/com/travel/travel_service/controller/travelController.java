package com.travel.travel_service.controller;

import com.travel.travel_service.dto.TravelRequest;
import com.travel.travel_service.dto.TravelResponse;
import com.travel.travel_service.dto.TravelPageResponse;
import com.travel.travel_service.services.TravelService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class travelController {

    private final TravelService travelService;

    public travelController(TravelService travelService) {
        this.travelService = travelService;
    }

    @GetMapping
    public TravelPageResponse listAll(@RequestParam(defaultValue = "0") int page) {
        return travelService.getTravelPage(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TravelResponse> getById(@PathVariable Long id) {
        return travelService.getTravel(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TravelResponse> create(@Valid @RequestBody TravelRequest request) {
        TravelResponse created = travelService.createTravel(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/travels/" + created.travelId()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TravelResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TravelRequest request) {
        return travelService.updateTravel(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!travelService.deleteTravel(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
