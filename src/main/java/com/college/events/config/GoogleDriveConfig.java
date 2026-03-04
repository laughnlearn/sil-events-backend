package com.college.events.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleDriveConfig {

    @Value("${google.drive.service-account-json-path:}")
    private String serviceAccountJsonPath;

    @Value("${google.drive.service-account-json-base64:}")
    private String serviceAccountJsonBase64;

    @Bean
    public Drive googleDriveClient() throws IOException, GeneralSecurityException {
        GoogleCredentials credentials = loadCredentials();
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        return new Drive.Builder(transport, GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                .setApplicationName("College Events Resources")
                .build();
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (serviceAccountJsonBase64 != null && !serviceAccountJsonBase64.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(serviceAccountJsonBase64);
            return ServiceAccountCredentials.fromStream(new ByteArrayInputStream(decoded))
                    .createScoped(List.of(DriveScopes.DRIVE));
        }

        if (serviceAccountJsonPath != null && !serviceAccountJsonPath.isBlank()) {
            return ServiceAccountCredentials.fromStream(new FileInputStream(serviceAccountJsonPath))
                    .createScoped(List.of(DriveScopes.DRIVE));
        }

        throw new IllegalStateException("Google Drive service account credentials are not configured");
    }
}
