package com.college.events.controller;

import com.college.events.dto.event.CreateEventRequest;
import com.college.events.dto.event.EventResponse;
import com.college.events.security.AppUserPrincipal;
import com.college.events.service.EventService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> list(
            @RequestParam(value = "club", required = false) String club,
            @RequestParam(value = "search", required = false) String search
    ) {
        return ResponseEntity.ok(eventService.getPublicEvents(club, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> detail(@PathVariable("id") Long id) {
        return ResponseEntity.ok(eventService.getPublicEventById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN1','ADMIN2')")
    public ResponseEntity<EventResponse> create(
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateEventRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request, principal));
    }

    @PostMapping(value = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN1','ADMIN2')")
    public ResponseEntity<EventResponse> uploadFiles(
            @PathVariable("id") Long id,
            @RequestPart("files") MultipartFile[] files,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(eventService.uploadFiles(id, files, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN1','ADMIN2')")
    public ResponseEntity<Void> delete(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        eventService.deleteEvent(id, principal);
        return ResponseEntity.noContent().build();
    }
}
