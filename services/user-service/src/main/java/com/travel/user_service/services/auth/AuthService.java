package com.travel.user_service.services.auth;

import com.travel.user_service.dto.login.AuthResponse;
import com.travel.user_service.dto.login.LoginRequest;
import com.travel.user_service.dto.register.RegisterRequest;
import com.travel.user_service.dto.register.RegisterResponse;
import com.travel.user_service.exceptions.BadRequestException;
import com.travel.user_service.models.Role;
import com.travel.user_service.models.User;
import com.travel.user_service.repository.UserRepository;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.springframework.dao.DuplicateKeyException;
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
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateKeyException("Email already registered: " + email);
        }

        User user = new User();
        user.setEmail(email);
        user.setFullName(request.getFullName().trim());
        user.setRole(Role.TRAVELER);
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
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        if (Boolean.FALSE.equals(user.getState())) {
            throw new BadRequestException("Account is deactivated. Please contact an administrator.");
        }

        String token = jwtService.generateToken(user);
        AuthResponse res = new AuthResponse();
        res.setToken(token);
        res.setState(user.getState());
        res.setRole(user.getRole().name());
        return res;
    }
}
