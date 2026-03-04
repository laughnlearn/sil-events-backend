package com.college.events.dto.admin;

public record CreateAdmin2Response(
        AdminUserResponse user,
        String temporaryPassword
) {
}
