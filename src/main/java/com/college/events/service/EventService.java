package com.college.events.service;

import com.college.events.domain.Event;
import com.college.events.domain.EventFile;
import com.college.events.domain.Role;
import com.college.events.domain.User;
import com.college.events.dto.event.CreateEventRequest;
import com.college.events.dto.event.EventFileResponse;
import com.college.events.dto.event.EventResponse;
import com.college.events.exception.BadRequestException;
import com.college.events.exception.ForbiddenException;
import com.college.events.exception.NotFoundException;
import com.college.events.repository.EventFileRepository;
import com.college.events.repository.EventRepository;
import com.college.events.repository.UserRepository;
import com.college.events.security.AppUserPrincipal;
import com.college.events.util.MimeTypeUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventFileRepository eventFileRepository;
    private final UserRepository userRepository;
    private final GoogleDriveService googleDriveService;
    private final TransactionTemplate transactionTemplate;

    public EventService(
            EventRepository eventRepository,
            EventFileRepository eventFileRepository,
            UserRepository userRepository,
            GoogleDriveService googleDriveService,
            PlatformTransactionManager transactionManager
    ) {
        this.eventRepository = eventRepository;
        this.eventFileRepository = eventFileRepository;
        this.userRepository = userRepository;
        this.googleDriveService = googleDriveService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getPublicEvents(String club, String search) {
        String normalizedClub = normalizeOrEmpty(club);
        String normalizedSearch = normalizeOrEmpty(search);
        List<Event> events = eventRepository.findActiveEvents(normalizedClub, normalizedSearch, LocalDateTime.now());
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, List<EventFileResponse>> resourcesByEventId = mapResourcesByEventId(eventIds);

        return events.stream()
                .map(event -> toResponse(event, resourcesByEventId.getOrDefault(event.getId(), List.of())))
                .toList();
    }

    public EventResponse getPublicEventById(Long eventId) {
        Event event = eventRepository.findWithFilesById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (event.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new NotFoundException("Event not found");
        }
        return toResponse(event);
    }

    public EventResponse createEvent(CreateEventRequest request, AppUserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        String clubName;
        if (principal.getRole() == Role.ADMIN2) {
            clubName = user.getClubName();
        } else {
            clubName = normalize(request.clubName());
            if (clubName == null) {
                throw new BadRequestException("clubName is required for ADMIN1");
            }
        }

        Event event = new Event();
        event.setClubName(clubName);
        event.setEventName(request.eventName().trim());
        event.setEventDate(request.eventDate());
        event.setEventTime(request.eventTime());
        event.setRoomNumber(request.roomNumber().trim());
        event.setCreatedByUser(user);
        event.setCreatedAt(LocalDateTime.now());
        event.setExpiresAt(event.getCreatedAt().plusHours(48));

        return toResponse(eventRepository.save(event));
    }

    public EventResponse uploadFiles(Long eventId, MultipartFile[] files, AppUserPrincipal principal) {
        if (files == null || files.length == 0) {
            throw new BadRequestException("At least one file is required");
        }

        Event event = eventRepository.findWithFilesById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        ensureCanManageEvent(principal, event);

        List<EventFile> newFiles = new ArrayList<>();
        List<String> uploadedDriveIds = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                String mimeType = file.getContentType();
                if (!MimeTypeUtil.isSupported(mimeType)) {
                    throw new BadRequestException("Unsupported file type: " + mimeType);
                }
                if (file.getSize() == 0) {
                    throw new BadRequestException("Empty file is not allowed");
                }

                DriveUploadResult uploadResult = googleDriveService.uploadFile(file);
                uploadedDriveIds.add(uploadResult.fileId());

                EventFile eventFile = new EventFile();
                eventFile.setEvent(event);
                eventFile.setFileName(file.getOriginalFilename());
                eventFile.setMimeType(mimeType);
                eventFile.setDriveFileId(uploadResult.fileId());
                eventFile.setDriveWebViewLink(uploadResult.webViewLink());
                eventFile.setUploadedAt(LocalDateTime.now());
                newFiles.add(eventFile);
            }

            transactionTemplate.executeWithoutResult(status -> eventFileRepository.saveAll(newFiles));
            event.getFiles().addAll(newFiles);
            return toResponse(event);
        } catch (RuntimeException ex) {
            for (String driveId : uploadedDriveIds) {
                try {
                    googleDriveService.deleteFile(driveId);
                } catch (Exception ignored) {
                    log.warn("Rollback cleanup failed for Drive file {}", driveId);
                }
            }
            throw ex;
        }
    }

    public void deleteEvent(Long eventId, AppUserPrincipal principal) {
        Event event = eventRepository.findWithFilesById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        ensureCanDeleteEvent(principal, event);
        deleteEventInternal(event);
    }

    public void deleteExpiredEvent(Long eventId) {
        Event event = eventRepository.findWithFilesById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        deleteEventInternal(event);
    }

    private void deleteEventInternal(Event event) {
        List<String> driveFileIds = event.getFiles().stream()
                .map(EventFile::getDriveFileId)
                .toList();

        for (String driveFileId : driveFileIds) {
            googleDriveService.deleteFile(driveFileId);
        }

        transactionTemplate.executeWithoutResult(status -> {
            eventFileRepository.deleteByEventId(event.getId());
            eventRepository.deleteById(event.getId());
        });
    }

    private void ensureCanManageEvent(AppUserPrincipal principal, Event event) {
        if (principal.getRole() == Role.ADMIN1) {
            return;
        }
        if (!event.getCreatedByUser().getId().equals(principal.getId())) {
            throw new ForbiddenException("ADMIN2 can only manage their own events");
        }
    }

    private void ensureCanDeleteEvent(AppUserPrincipal principal, Event event) {
        if (principal.getRole() == Role.ADMIN1) {
            return;
        }
        if (principal.getRole() == Role.ADMIN2 && event.getCreatedByUser().getId().equals(principal.getId())) {
            return;
        }
        throw new ForbiddenException("You do not have permission to delete this event");
    }

    private EventResponse toResponse(Event event) {
        List<EventFileResponse> resources = event.getFiles().stream()
                .sorted(Comparator.comparing(EventFile::getUploadedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(f -> new EventFileResponse(
                        f.getId(),
                        f.getFileName(),
                        f.getMimeType(),
                        f.getDriveFileId(),
                        f.getDriveWebViewLink(),
                        f.getUploadedAt()
                ))
                .toList();

        return toResponse(event, resources);
    }

    private EventResponse toResponse(Event event, List<EventFileResponse> resources) {
        return new EventResponse(
                event.getId(),
                event.getClubName(),
                event.getEventName(),
                event.getEventDate(),
                event.getEventTime(),
                event.getRoomNumber(),
                event.getCreatedByUser().getId(),
                event.getCreatedAt(),
                event.getExpiresAt(),
                resources
        );
    }

    private Map<Long, List<EventFileResponse>> mapResourcesByEventId(List<Long> eventIds) {
        List<EventFile> files = eventFileRepository.findByEventIdInOrderByEventIdAscUploadedAtAsc(eventIds);
        if (files.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<EventFileResponse>> resourcesByEventId = new HashMap<>();
        for (EventFile file : files) {
            EventFileResponse response = new EventFileResponse(
                    file.getId(),
                    file.getFileName(),
                    file.getMimeType(),
                    file.getDriveFileId(),
                    file.getDriveWebViewLink(),
                    file.getUploadedAt()
            );
            resourcesByEventId.computeIfAbsent(file.getEvent().getId(), ignored -> new ArrayList<>()).add(response);
        }
        return resourcesByEventId;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String normalizeOrEmpty(String value) {
        String normalized = normalize(value);
        return normalized == null ? "" : normalized;
    }
}
