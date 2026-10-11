package com.travel.user_service.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.travel.user_service.repository.UserRepository;
import com.travel.user_service.services.UserService;

import lombok.Data;

@Data
@Configuration
public class AdminInitializer {
    private final UserRepository userRepo;

    @Bean
    public CommandLineRunner initializeAdmin(UserService userService) {
        return args -> {
            boolean adminExists = userRepo.existsByEmail("admin@admin.com");
            if (!adminExists) {
                userService.createAdmin();
            }
        };
    }
}
