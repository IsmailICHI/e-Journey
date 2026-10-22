package com.travel.user_service.dto.register;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Please provide a valid email address")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 20, message = "Full name must be between 2 and 20 characters")
    @Pattern(regexp = "^[\\p{L} .'-]+$", message = "Full name must only contain letters, spaces, hyphens, and apostrophes")
    private String fullName;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 20, message = "Password must be at least 6 characters")
    private String password;

}
