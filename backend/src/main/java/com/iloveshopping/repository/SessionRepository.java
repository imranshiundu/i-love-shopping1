package com.iloveshopping.repository;

import com.iloveshopping.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, String> {

    Optional<Session> findByRefreshTokenHash(String refreshTokenHash);

    List<Session> findByUserId(String userId);

    List<Session> findByUserIdAndRevokedAtIsNull(String userId);

    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.revokedAt = :now WHERE s.userId = :userId AND s.revokedAt IS NULL")
    int revokeAllUserSessions(@Param("userId") String userId, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.revokedAt = :now WHERE s.id = :sessionId AND s.revokedAt IS NULL")
    int revokeSession(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM Session s WHERE s.expiresAt < :now OR s.revokedAt IS NOT NULL")
    int cleanupExpiredSessions(@Param("now") LocalDateTime now);
}