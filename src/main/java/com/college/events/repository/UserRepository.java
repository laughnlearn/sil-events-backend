package com.college.events.repository;

import com.college.events.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByClubNameIgnoreCase(String clubName);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByClubNameIgnoreCase(String clubName);
}
