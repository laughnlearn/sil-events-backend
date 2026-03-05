package com.college.events.service;

import com.college.events.domain.PasswordResetToken;
import com.college.events.domain.User;
import com.college.events.dto.auth.AuthResponse;
import com.college.events.dto.auth.ForgotPasswordRequest;
import com.college.events.dto.auth.LoginRequest;
import com.college.events.dto.auth.MessageResponse;
import com.college.events.dto.auth.ResetPasswordRequest;
import com.college.events.dto.auth.UserProfileResponse;
import com.college.events.exception.BadRequestException;
import com.college.events.exception.UnauthorizedException;
import com.college.events.repository.PasswordResetTokenRepository;
import com.college.events.repository.UserRepository;
import com.college.events.security.AppUserPrincipal;
import com.college.events.security.JwtService;
import com.college.events.util.TokenUtil;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final String frontendBaseUrl;

    public AuthService(
            JwtService jwtService,
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            @Value("${app.frontend-base-url:http://localhost:3000}") String frontendBaseUrl
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.email().trim();
        User user = resolveUserByIdentifier(identifier);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        AppUserPrincipal principal = new AppUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.getClubName()
        );

        String token = jwtService.generateToken(principal);
        return new AuthResponse(token, new UserProfileResponse(
                principal.getId(),
                principal.getUsername(),
                principal.getRole(),
                principal.getClubName()
        ));
    }

    public UserProfileResponse me(AppUserPrincipal principal) {
        return new UserProfileResponse(
                principal.getId(),
                principal.getUsername(),
                principal.getRole(),
                principal.getClubName()
        );
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(this::createTokenAndSendEmail);
        return new MessageResponse("If the email exists, a reset link has been sent.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String tokenHash = TokenUtil.sha256(request.token().trim());
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(tokenHash, LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("Token is invalid or expired"));

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        resetToken.setUsedAt(LocalDateTime.now());

        return new MessageResponse("Password has been reset successfully.");
    }

    private void createTokenAndSendEmail(User user) {
        passwordResetTokenRepository.deleteByUserIdAndUsedAtIsNull(user.getId());

        String rawToken = TokenUtil.randomToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(TokenUtil.sha256(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        passwordResetTokenRepository.save(token);

        String resetLink = frontendBaseUrl + "/admin/reset-password?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String recipientName = user.getClubName() != null ? user.getClubName() : user.getEmail();
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink, recipientName);
        } catch (RuntimeException ex) {
            log.warn("Password reset email failed for {}", user.getEmail(), ex);
            throw new BadRequestException("Unable to send reset email. Check MAIL_USERNAME/MAIL_PASSWORD/APP_MAIL_FROM.");
        }
    }

    private User resolveUserByIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return userRepository.findByEmailIgnoreCase(identifier).orElse(null);
        }

        return userRepository.findByClubNameIgnoreCase(identifier)
                .or(() -> userRepository.findByEmailIgnoreCase(identifier))
                .orElse(null);
    }
}
