package com.college.events.dto.auth;

public record AuthResponse(
        String accessToken,
        UserProfileResponse user
) {
}
