package com.travel.user_service.dto.login;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data

public class LoginRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

}
