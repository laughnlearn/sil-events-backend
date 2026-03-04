package com.college.events.service;

import com.college.events.domain.Role;
import com.college.events.domain.User;
import com.college.events.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Admin1Initializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin1.email:}")
    private String admin1Email;

    @Value("${app.admin1.password:}")
    private String admin1Password;

    public Admin1Initializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (admin1Email == null || admin1Email.isBlank() || admin1Password == null || admin1Password.isBlank()) {
            return;
        }

        if (userRepository.existsByEmailIgnoreCase(admin1Email)) {
            return;
        }

        User admin1 = new User();
        admin1.setRole(Role.ADMIN1);
        admin1.setEmail(admin1Email.trim().toLowerCase());
        admin1.setPasswordHash(passwordEncoder.encode(admin1Password));
        userRepository.save(admin1);
    }
}
