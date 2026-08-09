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
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySku(String sku);

    List<Product> findByCategoryId(String categoryId);

    List<Product> findByBrandId(String brandId);

    Page<Product> findByCategoryId(String categoryId, Pageable pageable);

    Page<Product> findByBrandId(String brandId, Pageable pageable);

    List<Product> findByIsActiveTrue();

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.categoryId IN :categoryIds")
    List<Product> findByCategoryIds(@Param("categoryIds") List<String> categoryIds);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.brandId IN :brandIds")
    List<Product> findByBrandIds(@Param("brandIds") List<String> brandIds);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.price BETWEEN :minPrice AND :maxPrice")
    List<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Product> searchByText(@Param("query") String query);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.stock > 0")
    List<Product> findInStockProducts();

    Page<Product> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.categoryId = :categoryId ORDER BY p.createdAt DESC")
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

    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN Category c ON p.category.parent = c " +
           "WHERE p.isActive = true " +
           "AND (:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:categorySlugs IS NULL OR p.category.slug IN :categorySlugs " +
           "     OR c.slug IN :categorySlugs) " +
           "AND (:brandSlugs IS NULL OR p.brand.slug IN :brandSlugs) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:inStockOnly = false OR p.stock > 0) " +
           "AND (:onSaleOnly = false OR (p.compareAtPrice IS NOT NULL AND p.compareAtPrice > p.price))")
    Page<Product> search(
            @Param("query") String query,
            @Param("categorySlugs") List<String> categorySlugs,
            @Param("brandSlugs") List<String> brandSlugs,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("inStockOnly") boolean inStockOnly,
            @Param("onSaleOnly") boolean onSaleOnly,
            Pageable pageable
    );

    @Query("SELECT DISTINCT p.name FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT(:prefix, '%')) AND p.isActive = true ORDER BY p.name")
    List<String> findTopByNameStartsWithIgnoreCase(@Param("prefix") String prefix, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p " +
           "WHERE p.id != :productId AND (p.categoryId = :categoryId OR p.brandId = :brandId) " +
           "AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Product> findSimilarProducts(@Param("productId") String productId,
                                       @Param("categoryId") String categoryId,
                                       @Param("brandId") String brandId,
                                       Pageable pageable);
}