package com.travel.user_service.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {

    @NotBlank(message = "Role is required")
    private String role;
}
