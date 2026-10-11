package com.travel.user_service.services;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.travel.user_service.dto.admin.UsersDto;
import com.travel.user_service.models.Role;

import com.travel.user_service.models.User;
import com.travel.user_service.repository.UserRepository;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User createAdmin() {
        User admin = new User();
        admin.setEmail("admin@admin.com");
        admin.setFullName("admin");
        admin.setPassword(passwordEncoder.encode("Admin123456@@"));

        admin.setRole(Role.Admin);
        admin.setState(true);

        return userRepository.save(admin);

    }

    public List<UsersDto> getAllUsers() {
        List<User> usersForAdmin = userRepository.findAll();
        List<UsersDto> usersData = usersForAdmin.stream()
                .map(u -> new UsersDto(
                        u.getEmail(),
                        u.getFullName(),
                        u.getRole().name(),
                        u.getState()))
                .toList();
        return usersData;
    }
}
