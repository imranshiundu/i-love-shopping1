package com.iloveshopping.service;

import com.iloveshopping.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Housekeeping: expired login sessions (and the 2FA enrollment placeholders)
 * linger in the sessions table forever. Purge them hourly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionCleanupService {

    private final SessionRepository sessionRepository;

    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void purgeExpiredSessions() {
        try {
            int removed = sessionRepository.cleanupExpiredSessions(LocalDateTime.now());
            if (removed > 0) {
                log.info("Purged {} expired/revoked session(s)", removed);
            }
        } catch (Exception e) {
            log.error("Session cleanup failed: {}", e.getMessage());
        }
    }
}
