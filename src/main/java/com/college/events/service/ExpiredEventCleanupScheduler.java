package com.college.events.service;

import com.college.events.repository.EventRepository;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExpiredEventCleanupScheduler {

    private final EventRepository eventRepository;
    private final EventService eventService;

    public ExpiredEventCleanupScheduler(EventRepository eventRepository, EventService eventService) {
        this.eventRepository = eventRepository;
        this.eventService = eventService;
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void cleanupExpiredEvents() {
        var expiredEvents = eventRepository.findByExpiresAtBefore(LocalDateTime.now());
        if (expiredEvents.isEmpty()) {
            return;
        }

        log.info("Expired event cleanup started. total={}", expiredEvents.size());
        int success = 0;
        for (var event : expiredEvents) {
            try {
                eventService.deleteExpiredEvent(event.getId());
                success++;
            } catch (Exception ex) {
                log.error("Failed to clean up expired event {}", event.getId(), ex);
            }
        }
        log.info("Expired event cleanup finished. deleted={} failed={}", success, expiredEvents.size() - success);
    }
}
