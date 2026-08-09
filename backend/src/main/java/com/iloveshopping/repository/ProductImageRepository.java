package com.iloveshopping.repository;

import com.iloveshopping.entity.ProductImage;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, String> {

    List<ProductImage> findByProductIdOrderBySortOrderAsc(String productId);

    Optional<ProductImage> findFirstByProductIdOrderBySortOrderAsc(String productId);

    @Modifying
    @Query("DELETE FROM ProductImage pi WHERE pi.productId = :productId")
    int deleteByProductId(@Param("productId") String productId);
}