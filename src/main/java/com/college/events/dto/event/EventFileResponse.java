package com.college.events.dto.event;

import java.time.LocalDateTime;

public record EventFileResponse(
        Long id,
        String fileName,
        String mimeType,
        String driveFileId,
        String driveWebViewLink,
        LocalDateTime uploadedAt
) {
}
