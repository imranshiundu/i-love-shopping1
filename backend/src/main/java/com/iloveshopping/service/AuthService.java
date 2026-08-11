package com.iloveshopping.service;

import com.iloveshopping.config.JwtProperties;
import com.iloveshopping.config.TwoFactorConfig;
import com.iloveshopping.dto.auth.*;
import com.iloveshopping.dto.user.ChangePasswordRequest;
import com.iloveshopping.dto.user.UpdateProfileRequest;
import com.iloveshopping.entity.Session;
import com.iloveshopping.entity.User;
import com.iloveshopping.exception.AuthenticationException;
import com.iloveshopping.exception.ResourceConflictException;
import com.iloveshopping.exception.ResourceNotFoundException;
import com.iloveshopping.repository.SessionRepository;
import com.iloveshopping.repository.UserRepository;
import com.iloveshopping.security.JwtService;
import com.iloveshopping.util.CaptchaUtil;
import com.iloveshopping.util.TwoFactorAuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaUtil captchaUtil;
    private final EmailService emailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());

        if (!captchaUtil.verify(request.getCaptchaToken())) {
            throw AuthenticationException.invalidCredentials();
        }

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ResourceConflictException("User with this email already exists");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordHash)
                .name(request.getName())
                .emailVerified(null)
                .twoFactorEnabled(false)
                .roles(Set.of(User.Role.USER))
                .build();

        userRepository.save(user);

        String verificationToken = UUID.randomUUID().toString();
        emailService.sendVerificationEmail(user.getEmail(), verificationToken);

        log.info("User registered successfully: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(null)
                .refreshToken(null)
                .expiresIn(0)
                .user(AuthResponse.UserDto.from(user))
                .twoFactorRequired(false)
                .message("Registration successful. Check your email for verification.")
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        log.info("Login attempt for email: {}", request.getEmail());

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(request.getEmail());
        if (userOpt.isEmpty()) {
            throw AuthenticationException.invalidCredentials();
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw AuthenticationException.invalidCredentials();
        }

        if (user.getEmailVerified() == null) {
            throw AuthenticationException.accountNotVerified();
        }

        if (user.getTwoFactorEnabled()) {
            String sessionId = createTempSessionFor2FA(user, ipAddress, userAgent);

            if (request.getTwoFactorCode() == null) {
                return AuthResponse.builder()
                        .twoFactorRequired(true)
                        .sessionId(sessionId)
                        .build();
            }

            if (!verifyTwoFactorCode(user.getTwoFactorSecret(), request.getTwoFactorCode())) {
                throw AuthenticationException.invalidTwoFactorCode();
            }

            sessionRepository.deleteById(sessionId);
        }

        String sessionId = UUID.randomUUID().toString();
        String refreshToken = jwtService.generateRefreshToken(user, sessionId);
        String refreshTokenHash = passwordEncoder.encode(refreshToken);

        sessionRepository.findByUserId(user.getId())
                .forEach(session -> sessionRepository.deleteById(session.getId()));

        Session session = Session.builder()
                .id(sessionId)
                .user(user)
                .refreshTokenHash(refreshTokenHash)
                .userAgent(userAgent)
                .ip(ipAddress)
                .expiresAt(LocalDateTime.now().plusDays(jwtProperties.getRefreshExpiryDays()))
                .build();

        sessionRepository.save(session);

        String accessToken = jwtService.generateAccessToken(user, sessionId);

        log.info("User logged in successfully: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getAccessExpiryMinutes() * 60)
                .user(AuthResponse.UserDto.from(user))
                .twoFactorRequired(false)
                .sessionId(sessionId)
                .build();
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        log.debug("Token refresh attempt");

        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw AuthenticationException.invalidToken();
        }

        JwtService.JwtClaims claims;
        try {
            claims = jwtService.parseRefreshToken(refreshToken);
        } catch (JwtService.InvalidTokenException e) {
            throw AuthenticationException.invalidToken();
        }

        User user = userRepository.findById(claims.getSubject())
                .orElseThrow(() -> AuthenticationException.invalidToken());

        Optional<Session> sessionOpt = sessionRepository.findById(claims.getSessionId());
        if (sessionOpt.isEmpty() || sessionOpt.get().isRevoked()) {
            throw AuthenticationException.invalidToken();
        }

        if (!passwordEncoder.matches(refreshToken, sessionOpt.get().getRefreshTokenHash())) {
            sessionRepository.revokeAllUserSessions(user.getId(), LocalDateTime.now());
            throw AuthenticationException.tokenExpired();
        }

        sessionRepository.revokeSession(sessionOpt.get().getId(), LocalDateTime.now());

        String newSessionId = UUID.randomUUID().toString();
        String newRefreshToken = jwtService.generateRefreshToken(user, newSessionId);
        String refreshTokenHash = passwordEncoder.encode(newRefreshToken);

        Session newSession = Session.builder()
                .id(newSessionId)
                .user(user)
                .refreshTokenHash(refreshTokenHash)
                .expiresAt(LocalDateTime.now().plusDays(jwtProperties.getRefreshExpiryDays()))
                .build();

        sessionRepository.save(newSession);

        String accessToken = jwtService.generateAccessToken(user, newSessionId);

        log.debug("Token refreshed successfully for user: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtProperties.getAccessExpiryMinutes() * 60)
                .user(AuthResponse.UserDto.from(user))
                .sessionId(newSessionId)
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        log.debug("Logout attempt");

        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        try {
            JwtService.JwtClaims claims = jwtService.parseRefreshToken(refreshToken);
            sessionRepository.revokeSession(claims.getSessionId(), LocalDateTime.now());
        } catch (Exception e) {
            log.debug("Logout with invalid token - proceeding");
        }
    }

    @Transactional
    public void logoutAllSessions(String userId) {
        log.info("Revoking all sessions for user: {}", userId);
        sessionRepository.revokeAllUserSessions(userId, LocalDateTime.now());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Password reset requested for: {}", request.getEmail());

        userRepository.findByEmailIgnoreCase(request.getEmail()).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Password reset with token");
    }

    @Transactional
    public void verifyEmail(String token) {
        log.info("Email verification attempt");
    }

    @Transactional
    public TwoFASetupResponse setup2FA(String userId) {
        log.info("Setting up 2FA for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String secret = generateTotpSecret();

        return TwoFASetupResponse.builder()
                .secret(secret)
                .qrCodeUrl(TwoFactorAuthUtil.getQrCodeUrl(user.getEmail(), secret))
                .manualEntryKey(secret)
                .build();
    }

    @Transactional
    public void enable2FA(String userId, Enable2FARequest request) {
        log.info("Enabling 2FA for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String secret = generateTotpSecret();

        if (!verifyTwoFactorCode(secret, request.getCode())) {
            throw AuthenticationException.invalidTwoFactorCode();
        }

        user.setTwoFactorEnabled(true);
        user.setTwoFactorSecret(secret);
        userRepository.save(user);

        log.info("2FA enabled for user: {}", user.getEmail());
    }

    @Transactional
    public void disable2FA(String userId, Disable2FARequest request) {
        log.info("Disabling 2FA for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw AuthenticationException.invalidCredentials();
        }

        if (!verifyTwoFactorCode(user.getTwoFactorSecret(), request.getCode())) {
            throw AuthenticationException.invalidTwoFactorCode();
        }

        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);

        log.info("2FA disabled for user: {}", user.getEmail());
    }

    @Transactional
    public void updateProfile(String userId, UpdateProfileRequest request) {
        log.info("Updating profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
                throw new ResourceConflictException("User with this email already exists");
            }
            user.setEmail(request.getEmail().toLowerCase());
            user.setEmailVerified(null);
        }

        userRepository.save(user);
        log.info("Profile updated for user: {}", user.getEmail());
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        log.info("Changing password for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw AuthenticationException.invalidCredentials();
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        sessionRepository.revokeAllUserSessions(userId, LocalDateTime.now());

        log.info("Password changed for user: {}", user.getEmail());
    }

    private String createTempSessionFor2FA(User user, String ipAddress, String userAgent) {
        String sessionId = UUID.randomUUID().toString();
        Session session = Session.builder()
                .id(sessionId)
                .user(user)
                .refreshTokenHash(null)
                .userAgent(userAgent)
                .ip(ipAddress)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        sessionRepository.save(session);
        return sessionId;
    }

    private String generateTotpSecret() {
        return TwoFactorAuthUtil.generateSecret();
    }

    private boolean verifyTwoFactorCode(String secret, String code) {
        return TwoFactorAuthUtil.verifyCode(secret, code);
    }
}