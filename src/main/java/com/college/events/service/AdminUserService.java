package com.college.events.service;

import com.college.events.domain.Role;
import com.college.events.domain.User;
import com.college.events.dto.admin.AdminUserResponse;
import com.college.events.dto.admin.CreateAdmin2Request;
import com.college.events.dto.admin.CreateAdmin2Response;
import com.college.events.exception.BadRequestException;
import com.college.events.exception.NotFoundException;
import com.college.events.repository.EventRepository;
import com.college.events.repository.UserRepository;
import com.college.events.util.TokenUtil;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AdminUserService(
            UserRepository userRepository,
            EventRepository eventRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public CreateAdmin2Response createAdmin2(CreateAdmin2Request request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BadRequestException("Email already exists");
        }

        String clubName = request.clubName().trim();
        if (userRepository.existsByClubNameIgnoreCase(clubName)) {
            throw new BadRequestException("Club name already exists");
        }

        String tempPassword = (request.tempPassword() == null || request.tempPassword().isBlank())
                ? TokenUtil.randomPassword()
                : request.tempPassword();

        User admin2 = new User();
        admin2.setRole(Role.ADMIN2);
        admin2.setClubName(clubName);
        admin2.setEmail(normalizedEmail);
        admin2.setPasswordHash(passwordEncoder.encode(tempPassword));

        User saved = userRepository.save(admin2);
        emailService.sendAdmin2CredentialsEmail(saved.getEmail(), saved.getClubName(), tempPassword);

        return new CreateAdmin2Response(toResponse(saved), tempPassword);
    }

    public List<AdminUserResponse> getAdminUsers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ADMIN2)
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteAdminUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getRole() == Role.ADMIN1) {
            throw new BadRequestException("Cannot delete ADMIN1 user");
        }

        if (eventRepository.existsByCreatedByUserId(userId)) {
            throw new BadRequestException("Cannot delete ADMIN2 user with events. Delete their events first.");
        }

        userRepository.delete(user);
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getClubName(),
                user.getCreatedAt()
        );
    }
}
