package com.travel.user_service.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.travel.user_service.dto.admin.UsersDto;
import com.travel.user_service.dto.admin.UserPageResponse;
import com.travel.user_service.dto.admin.UserStatisticsResponse;
import com.travel.user_service.dto.profile.UserProfileResponse;
import com.travel.user_service.models.Role;

import com.travel.user_service.models.User;
import com.travel.user_service.repository.UserRepository;
import com.travel.user_service.exceptions.ResourceNotFoundException;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Service
public class UserService {
    private static final int ADMIN_PAGE_SIZE = 5;

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

    public UserPageResponse getAllUsers(String currentEmail, int page) {
        Pageable pageable = PageRequest.of(Math.max(0, page), ADMIN_PAGE_SIZE, Sort.by("id").ascending());
        Page<User> users = userRepository.findAllExcludingEmail(currentEmail, pageable);
        List<UsersDto> usersData = users.getContent().stream()
                .map(u -> new UsersDto(
                        u.getId(),
                        u.getEmail(),
                        u.getFullName(),
                        u.getRole().name(),
                        u.getState()))
                .toList();
        return new UserPageResponse(
                usersData,
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages(),
                users.isLast());
    }

    public UserStatisticsResponse getUserStatistics(String currentEmail) {
        return new UserStatisticsResponse(
                userRepository.countExcludingEmail(currentEmail),
                userRepository.countActiveExcludingEmail(currentEmail));
    }

    public UserProfileResponse getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new UserProfileResponse(
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getState());
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }

    public boolean isSelf(Long id, String currentEmail) {
        return userRepository.findById(id)
                .map(user -> user.getEmail().equalsIgnoreCase(currentEmail))
                .orElse(false);
    }
}
