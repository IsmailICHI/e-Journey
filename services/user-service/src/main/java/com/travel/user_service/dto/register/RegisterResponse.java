package com.travel.user_service.dto.register;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

public class RegisterResponse {

    private String email;
    private String fullName;
    private String role;

}