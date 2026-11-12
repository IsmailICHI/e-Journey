package com.travel.user_service.controller;

import com.travel.user_service.dto.profile.UserProfileResponse;
import com.travel.user_service.dto.profile.PublicManagerResponse;
import com.travel.user_service.client.ManagerBookingClient;
import com.travel.user_service.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ManagerBookingClient managerBookingClient;

    @GetMapping("/me")
    public UserProfileResponse getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return userService.getUserProfile(jwt.getSubject());
    }

    @GetMapping("/public/managers/{managerId}")
    public PublicManagerResponse getPublicManagerProfile(@PathVariable Long managerId) {
        return userService.getPublicManagerProfile(managerId);
    }

    @GetMapping("/manager/bookings/{bookingId}/traveler-profile")
    @PreAuthorize("hasRole('TRAVEL_MANAGER')")
    public UserProfileResponse getTravelerProfileForBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal Jwt jwt) {
        Long travelerId = managerBookingClient.getTravelerId(
                bookingId, "Bearer " + jwt.getTokenValue());
        return userService.getUserProfile(travelerId);
    }
}
