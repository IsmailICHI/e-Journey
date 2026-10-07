package com.travel.user_service.dto.register;

import lombok.Data;

@Data
public class RegisterResponse {

    private String email;

    public RegisterResponse(String email) {
        this.email = email;
    }
}