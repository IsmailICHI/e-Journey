package com.travel.user_service.dto.register;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @NotEmpty
    @Size(min = 3, max = 15)
    private String fullName;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

}
