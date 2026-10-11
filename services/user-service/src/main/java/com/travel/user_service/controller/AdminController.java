package com.travel.user_service.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
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
    public List<UsersDto> getAllUsers() {
        return userService.getAllUsers();
    }
}
