package com.travel.user_service.dto.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    @NotBlank(message = "Full name is required")
    @Pattern (regexp = "^[\\p{L} .'-]+$", message = "Full name must only contain letters, spaces, hyphens, and apostrophes")
    @Size (min = 2, max = 20, message = "Full name must be between 2 and 20 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}