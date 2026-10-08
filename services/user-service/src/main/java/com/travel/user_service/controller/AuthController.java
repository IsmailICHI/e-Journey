package com.travel.user_service.controller;

import com.travel.user_service.dto.login.AuthResponse;
import com.travel.user_service.dto.login.LoginRequest;
import com.travel.user_service.dto.register.RegisterRequest;
import com.travel.user_service.dto.register.RegisterResponse;
import com.travel.user_service.services.auth.AuthService;
import jakarta.validation.Valid;
import lombok.Data;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Data
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(
            @Valid @RequestBody RegisterRequest request) {
        System.out.println("[REQUEST IN REGISTER] : " + request.toString());
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}