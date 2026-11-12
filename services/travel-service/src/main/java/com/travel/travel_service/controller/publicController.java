package com.travel.travel_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "10") int limit) {
        if (q.isBlank()) {
            return ResponseEntity.ok(travelService.getAllTravels());
        }
        return ResponseEntity.ok(travelService.searchTravels(q, limit));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<?> recommendations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "6") int limit) {
        Object claim = jwt.getClaim("userId");
        if (!(claim instanceof Number userId) || userId.longValue() <= 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authenticated token does not contain a valid user ID");
        }
        return ResponseEntity.ok(travelService.getRecommendations(userId.longValue(), limit));
    }

    @GetMapping("/managers/{managerId}/travels")
    public ResponseEntity<?> managerTravels(@org.springframework.web.bind.annotation.PathVariable Long managerId) {
        return ResponseEntity.ok(travelService.getManagerTravels(managerId));
    }

    @GetMapping("/travels/{id}")
    public ResponseEntity<?> getTravelForBooking(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return ResponseEntity.ok(travelService.getTravel(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found")));
    }
}
