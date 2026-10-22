package com.travel.user_service.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UsersDto {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private Boolean state;
}
