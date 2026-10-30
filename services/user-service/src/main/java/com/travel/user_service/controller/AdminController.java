package com.travel.user_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.travel.user_service.dto.admin.*;
import com.travel.user_service.dto.profile.UserProfileResponse;
import com.travel.user_service.services.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN','TRAVEL_MANAGER')")
@Validated
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    public UserPageResponse getAllUsers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index must be 0 or greater") int page) {
        return userService.getAllUsers(jwt.getSubject(), page);
    }

    @GetMapping("/users/statistics")
    public UserStatisticsResponse getUserStatistics(@AuthenticationPrincipal Jwt jwt) {
        return userService.getUserStatistics(jwt.getSubject());
    }

    @PatchMapping("/users/{id}/role")
    public UserProfileResponse updateUserRole(
            @PathVariable @Positive(message = "User ID must be a positive number") Long id,
            @RequestBody @Valid UpdateUserRoleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        if (userService.isSelf(id, jwt.getSubject())) {
            throw new AccessDeniedException("Administrators cannot change their own role");
        }
        return userService.updateUserRole(id, request.getRole());
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @PathVariable @Positive(message = "User ID must be a positive number") Long id,
            @AuthenticationPrincipal Jwt jwt) {
        if (userService.isSelf(id, jwt.getSubject())) {
            throw new AccessDeniedException("Administrators cannot delete their own account");
        }
        userService.deleteUser(id);
    }
}
