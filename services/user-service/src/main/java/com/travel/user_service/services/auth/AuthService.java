package com.travel.user_service.services.auth;

import com.travel.user_service.dto.login.AuthResponse;
import com.travel.user_service.dto.login.LoginRequest;
import com.travel.user_service.dto.register.RegisterRequest;
import com.travel.user_service.dto.register.RegisterResponse;
import com.travel.user_service.models.Role;
import com.travel.user_service.models.User;
import com.travel.user_service.repository.UserRepository;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.springframework.dao.DuplicateKeyException;
// import com.cinema.user.exceptions.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Data
@AllArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateKeyException("Email already registered");
        }

        User user = new User();

        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setRole(Role.User);
        user.setState(true);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);
        RegisterResponse res = new RegisterResponse();
        res.setEmail(user.getEmail());
        res.setFullName(user.getFullName());
        res.setRole(user.getRole().name());

        return res;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);
        AuthResponse res = new AuthResponse();
        res.setToken(token);
        res.setState(user.getState());
        res.setRole(user.getRole().name());
        return res;
    }
}
