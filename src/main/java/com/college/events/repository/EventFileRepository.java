package com.college.events.repository;

import com.college.events.domain.EventFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventFileRepository extends JpaRepository<EventFile, Long> {
    List<EventFile> findByEventId(Long eventId);

    void deleteByEventId(Long eventId);
}
