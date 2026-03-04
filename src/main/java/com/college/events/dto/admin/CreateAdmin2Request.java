package com.college.events.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAdmin2Request(
        @NotBlank @Size(min = 2, max = 100) String clubName,
        @NotBlank @Email String email,
        @Size(min = 8, max = 100) String tempPassword
) {
}
