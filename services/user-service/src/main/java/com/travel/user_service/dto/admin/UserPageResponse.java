package com.travel.user_service.dto.admin;

import java.util.List;

public record UserPageResponse(
        List<UsersDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {
}
