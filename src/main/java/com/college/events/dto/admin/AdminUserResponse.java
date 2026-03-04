package com.college.events.dto.admin;

import com.college.events.domain.Role;
import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String email,
        Role role,
        String clubName,
        LocalDateTime createdAt
) {
}
