package com.iloveshopping.repository;

import com.iloveshopping.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, String> {

    Optional<Cart> findByUserId(String userId);

    Optional<Cart> findBySessionId(String sessionId);

    boolean existsByUserId(String userId);

    boolean existsBySessionId(String sessionId);
}