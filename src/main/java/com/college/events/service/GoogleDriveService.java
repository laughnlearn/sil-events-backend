package com.college.events.service;

import com.college.events.exception.BadRequestException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class GoogleDriveService {

    private final Drive drive;
    private final String folderId;

    public GoogleDriveService(Drive drive, @Value("${google.drive.folder-id}") String folderId) {
        this.drive = drive;
        this.folderId = folderId;
    }

    public DriveUploadResult uploadFile(MultipartFile multipartFile) {
        try {
            File metadata = new File();
            metadata.setName(multipartFile.getOriginalFilename());
            metadata.setParents(List.of(folderId));

            ByteArrayContent content = new ByteArrayContent(multipartFile.getContentType(), multipartFile.getBytes());

            File uploaded = drive.files()
                    .create(metadata, content)
                    .setFields("id, webViewLink")
                    .execute();

            Permission permission = new Permission();
            permission.setType("anyone");
            permission.setRole("reader");
            drive.permissions().create(uploaded.getId(), permission).execute();

            return new DriveUploadResult(uploaded.getId(), uploaded.getWebViewLink());
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
}
