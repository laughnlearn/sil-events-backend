package com.college.events.dto.auth;

import com.college.events.domain.Role;

public record UserProfileResponse(
        Long id,
        String email,
        Role role,
        String clubName
) {
}
