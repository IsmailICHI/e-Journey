package com.travel.user_service.dto.login;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private String role;
    private Boolean state;

}