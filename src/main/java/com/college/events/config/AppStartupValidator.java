package com.college.events.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppStartupValidator {

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${google.drive.folder-id:}")
    private String driveFolderId;

    @PostConstruct
    void validate() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("APP_JWT_SECRET must be configured and Base64-encoded");
        }
        if (driveFolderId == null || driveFolderId.isBlank()) {
            throw new IllegalStateException("GOOGLE_DRIVE_FOLDER_ID must be configured");
        }
    }
}
