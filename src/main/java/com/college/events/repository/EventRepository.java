package com.college.events.repository;

import com.college.events.domain.Event;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("""
            select distinct e from Event e
            left join fetch e.files f
            where e.id = :eventId
            """)
    Optional<Event> findWithFilesById(@Param("eventId") Long eventId);

    @Query("""
            select distinct e from Event e
            left join fetch e.files f
            where e.expiresAt > :now
              and (:club is null or lower(e.clubName) = lower(:club))
              and (:search is null or lower(e.eventName) like lower(concat('%', :search, '%')))
            order by e.eventDate asc, e.eventTime asc
            """)
    List<Event> findActiveEvents(@Param("club") String club, @Param("search") String search, @Param("now") LocalDateTime now);

    List<Event> findByCreatedByUserIdAndExpiresAtAfterOrderByEventDateAscEventTimeAsc(Long userId, LocalDateTime now);

    List<Event> findByExpiresAtAfterOrderByEventDateAscEventTimeAsc(LocalDateTime now);

    List<Event> findByExpiresAtBefore(LocalDateTime now);

    boolean existsByCreatedByUserId(Long userId);
}
