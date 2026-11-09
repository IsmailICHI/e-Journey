package com.travel.travel_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.travel.travel_service.services.TravelService;

import lombok.RequiredArgsConstructor;

// import lombok.;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor 


public class publicController {

    // private final TravelRepository travelRepo;
    private final TravelService travelService;

    @GetMapping("/public")
    public ResponseEntity<?> publicEndpoint() {
        return ResponseEntity.ok(travelService.getAllTravels());
    }

    @GetMapping("/travels/{id}")
    public ResponseEntity<?> getTravelForBooking(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return ResponseEntity.ok(travelService.getTravel(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found")));
    }
}
