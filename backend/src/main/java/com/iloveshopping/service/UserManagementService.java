package com.iloveshopping.service;

import com.iloveshopping.dto.user.ChangePasswordRequest;
import com.iloveshopping.dto.user.UpdateProfileRequest;
import com.iloveshopping.dto.user.UserProfileResponse;
import com.iloveshopping.entity.User;
import com.iloveshopping.exception.AuthenticationException;
import com.iloveshopping.exception.ResourceConflictException;
import com.iloveshopping.exception.ResourceNotFoundException;
import com.iloveshopping.repository.SessionRepository;
import com.iloveshopping.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRepository sessionRepository;
    private final EmailService emailService;

    public UserProfileResponse getCurrentUserProfile() {
        User user = getCurrentUser();
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

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
            // New address must be verified before it is trusted: issue a
            // single-use token and mail the verification link.
            user.setEmailVerificationToken(java.util.UUID.randomUUID().toString());
            user.setEmailVerificationExpiresAt(java.time.LocalDateTime.now().plusHours(24));
            userRepository.save(user);
            emailService.sendVerificationEmail(user.getEmail(), user.getEmailVerificationToken());
            log.info("Profile updated with email change for user: {}", user.getEmail());
            return UserProfileResponse.from(user);
        }

        userRepository.save(user);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw AuthenticationException.invalidCredentials();
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Stolen-session protection: all other sessions die with the old password.
        sessionRepository.revokeAllUserSessions(user.getId(), java.time.LocalDateTime.now());

        log.info("Password changed for user: {}", user.getEmail());
    }

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return userRepository.findById(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", user.getId()));
        }
        throw new AuthenticationException("User not authenticated");
    }
}