package com.iloveshopping.security;

import com.iloveshopping.entity.Session;
import com.iloveshopping.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtTokenUtil {

    private final JwtService jwtService;
    private final SessionRepository sessionRepository;

    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.iloveshopping.entity.User user) {
            return user.getId();
        }
        return null;
    }

    public boolean isValidSession(String sessionId) {
        if (sessionId == null) return false;
        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);
        return sessionOpt.isPresent() && !sessionOpt.get().isRevoked();
    }
}