package com.travel.user_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.travel.user_service.dto.admin.*;
import com.travel.user_service.services.UserService;

import lombok.Data;

@Data
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    public UserPageResponse getAllUsers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page) {
        return userService.getAllUsers(jwt.getSubject(), page);
    }

    @GetMapping("/users/statistics")
    public UserStatisticsResponse getUserStatistics(@AuthenticationPrincipal Jwt jwt) {
        return userService.getUserStatistics(jwt.getSubject());
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        if (userService.isSelf(id, jwt.getSubject())) {
            throw new AccessDeniedException("Administrators cannot delete their own account");
        }
        userService.deleteUser(id);
    }
}
