package com.college.events.service;

import com.college.events.exception.BadRequestException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class GoogleDriveService {

    private static final Pattern FOLDER_URL_PATTERN = Pattern.compile("/folders/([a-zA-Z0-9_-]+)");

    private final Drive drive;
    private final String folderId;

    public GoogleDriveService(Drive drive, @Value("${google.drive.folder-id}") String folderId) {
        this.drive = drive;
        this.folderId = normalizeFolderId(folderId);
    }

    public DriveUploadResult uploadFile(MultipartFile multipartFile) {
        try {
            File metadata = new File();
            metadata.setName(multipartFile.getOriginalFilename());
            metadata.setParents(List.of(folderId));

            String contentType = multipartFile.getContentType() == null
                    ? "application/octet-stream"
                    : multipartFile.getContentType();
            ByteArrayContent content = new ByteArrayContent(contentType, multipartFile.getBytes());

            File uploaded = drive.files()
                    .create(metadata, content)
                    .setFields("id, webViewLink")
                    .setSupportsAllDrives(true)
                    .execute();

            Permission permission = new Permission();
            permission.setType("anyone");
            permission.setRole("reader");
            drive.permissions()
                    .create(uploaded.getId(), permission)
                    .setSupportsAllDrives(true)
                    .execute();

            return new DriveUploadResult(uploaded.getId(), uploaded.getWebViewLink());
        } catch (GoogleJsonResponseException ex) {
            String apiMessage = ex.getDetails() != null ? ex.getDetails().getMessage() : ex.getMessage();
            log.error("Google Drive upload failed (status {}): {}", ex.getStatusCode(), apiMessage, ex);
            throw new BadRequestException("Google Drive upload failed: " + apiMessage);
        } catch (IOException ex) {
            log.error("Google Drive upload failed", ex);
            throw new BadRequestException("Failed to upload file to Google Drive");
        }
    }

    public void deleteFile(String driveFileId) {
        try {
            drive.files().delete(driveFileId).execute();
        } catch (GoogleJsonResponseException ex) {
            if (ex.getStatusCode() == 404) {
                log.info("Drive file {} already deleted", driveFileId);
                return;
            }
            log.warn("Failed to delete Drive file {}: {}", driveFileId, ex.getMessage());
            throw new BadRequestException("Failed to delete file from Google Drive: " + driveFileId);
        } catch (IOException ex) {
            log.warn("Failed to delete Drive file {}: {}", driveFileId, ex.getMessage());
            throw new BadRequestException("Failed to delete file from Google Drive: " + driveFileId);
        }
    }

    private String normalizeFolderId(String rawFolderId) {
        if (rawFolderId == null) {
            return null;
        }

        String trimmed = rawFolderId.trim();
        if (trimmed.isBlank()) {
            return trimmed;
        }

        Matcher matcher = FOLDER_URL_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return trimmed;
    }
}
