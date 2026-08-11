package com.iloveshopping.repository;

import com.iloveshopping.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySku(String sku);

    List<Product> findByCategoryId(String categoryId);

    List<Product> findByBrandId(String brandId);

    Page<Product> findByCategoryId(String categoryId, Pageable pageable);

    Page<Product> findByBrandId(String brandId, Pageable pageable);

    List<Product> findByIsActiveTrue();

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.stock > 0")
    List<Product> findInStockProducts();

    Page<Product> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.category.id = :categoryId ORDER BY p.createdAt DESC")
    Page<Product> findByCategoryIdOrderByCreatedAtDesc(@Param("categoryId") String categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.price ASC")
    List<Product> findByPriceAsc();

    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.price DESC")
    List<Product> findByPriceDesc();

    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.createdAt DESC")
    List<Product> findLatestProducts();

    boolean existsBySlug(String slug);

    boolean existsBySku(String sku);

    boolean existsBySlugAndIdNot(String slug, String id);

    boolean existsBySkuAndIdNot(String sku, String id);

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :productId AND p.stock >= :quantity")
    int decrementStock(@Param("productId") String productId, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.id = :productId")
    int incrementStock(@Param("productId") String productId, @Param("quantity") int quantity);

    @Query("""
            SELECT p FROM Product p
            LEFT JOIN FETCH p.category cat
            LEFT JOIN FETCH cat.parent
            LEFT JOIN FETCH p.brand
            WHERE p.isActive = true
            """)
    List<Product> findAllActive();

    @Query("""
            SELECT p FROM Product p
            LEFT JOIN FETCH p.category cat
            LEFT JOIN FETCH cat.parent
            LEFT JOIN FETCH p.brand
            WHERE p.id = :productId
            """)
    Optional<Product> findProductWithDetails(@Param("productId") String productId);

    @Query(value = "SELECT DISTINCT p.name FROM products p WHERE p.name ILIKE (:prefix || '%') AND p.is_active = true ORDER BY p.name",
           nativeQuery = true)
    List<String> findTopByNameStartsWithIgnoreCase(@Param("prefix") String prefix, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p " +
           "WHERE p.id != :productId AND (p.category.id = :categoryId OR p.brand.id = :brandId) " +
           "AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Product> findSimilarProducts(@Param("productId") String productId,
                                       @Param("categoryId") String categoryId,
                                       @Param("brandId") String brandId,
                                       Pageable pageable);
}
