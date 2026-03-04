package com.college.events.dto.event;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateEventRequest(
        @Size(max = 100) String clubName,
        @NotBlank @Size(min = 2, max = 200) String eventName,
        @NotNull @FutureOrPresent LocalDate eventDate,
        @NotNull LocalTime eventTime,
        @NotBlank @Size(max = 50) String roomNumber
) {
}
