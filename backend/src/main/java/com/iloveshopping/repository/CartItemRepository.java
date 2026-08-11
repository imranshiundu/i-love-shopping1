package com.iloveshopping.repository;

import com.iloveshopping.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, String> {

    List<CartItem> findByCartId(String cartId);

    Optional<CartItem> findByCartIdAndProductIdAndVariantId(String cartId, String productId, String variantId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    int deleteByCartId(@Param("cartId") String cartId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem ci WHERE ci.id = :id")
    int deleteCartItemById(@Param("id") String id);
}