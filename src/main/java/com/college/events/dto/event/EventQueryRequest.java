package com.college.events.dto.event;

import jakarta.validation.constraints.Size;

public record EventQueryRequest(
        @Size(max = 100) String club,
        @Size(max = 100) String search
) {
}
