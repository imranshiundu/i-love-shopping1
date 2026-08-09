package com.iloveshopping.repository;

import com.iloveshopping.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT u FROM User u WHERE :role MEMBER OF u.roles")
    List<User> findByRole(User.Role role);

    @Query("SELECT u FROM User u WHERE u.emailVerified IS NOT NULL")
    List<User> findVerifiedUsers();

    @Query("SELECT u FROM User u WHERE u.twoFactorEnabled = true")
    List<User> findUsersWith2FAEnabled();
}