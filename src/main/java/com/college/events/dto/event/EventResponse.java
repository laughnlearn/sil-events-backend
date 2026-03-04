package com.college.events.dto.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record EventResponse(
        Long id,
        String clubName,
        String eventName,
        LocalDate eventDate,
        LocalTime eventTime,
        String roomNumber,
        Long createdByUserId,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        List<EventFileResponse> resources
) {
}
