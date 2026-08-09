package com.iloveshopping.controller;

import com.iloveshopping.dto.catalog.BrandResponse;
import com.iloveshopping.dto.catalog.CategoryResponse;
import com.iloveshopping.dto.catalog.ProductResponse;
import com.iloveshopping.dto.catalog.ProductSearchResponse;
import com.iloveshopping.dto.common.ApiResponse;
import com.iloveshopping.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Product catalog, categories, and brands")
public class CatalogController {

    private final CatalogService catalogService;

    // ===== Categories =====

    @GetMapping("/categories")
    @Operation(summary = "Get all categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(
            @RequestParam(defaultValue = "true") boolean includeChildren) {

        List<CategoryResponse> categories = catalogService.getAllCategories(includeChildren);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/categories/{slug}")
    @Operation(summary = "Get category by slug")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(
            @Parameter(description = "Category slug")
            @PathVariable String slug) {

        CategoryResponse category = catalogService.getCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @GetMapping("/categories/{slug}/products")
    @Operation(summary = "Get products in a category")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getCategoryProducts(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "newest") String sortBy) {

        Page<ProductResponse> products = catalogService.getCategoryProducts(slug, page, size, sortBy);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    // ===== Brands =====

    @GetMapping("/brands")
    @Operation(summary = "Get all brands")
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getBrands() {

        List<BrandResponse> brands = catalogService.getAllBrands();
        return ResponseEntity.ok(ApiResponse.success(brands));
    }

    @GetMapping("/brands/{slug}")
    @Operation(summary = "Get brand by slug")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrand(
            @Parameter(description = "Brand slug")
            @PathVariable String slug) {

        BrandResponse brand = catalogService.getBrandBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(brand));
    }

    @GetMapping("/brands/{slug}/products")
    @Operation(summary = "Get products from a brand")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getBrandProducts(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "newest") String sortBy) {

        Page<ProductResponse> products = catalogService.getBrandProducts(slug, page, size, sortBy);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    // ===== Products =====

    @GetMapping("/products")
    @Operation(summary = "Search and filter products")
    public ResponseEntity<ApiResponse<ProductSearchResponse>> searchProducts(
            @Parameter(description = "Search query")
            @RequestParam(required = false) String query,
            @Parameter(description = "Category slugs to filter by")
            @RequestParam(required = false) List<String> categories,
            @Parameter(description = "Brand slugs to filter by")
            @RequestParam(required = false) List<String> brands,
            @Parameter(description = "Minimum price")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Only show in-stock items")
            @RequestParam(defaultValue = "false") boolean inStockOnly,
            @Parameter(description = "Only show sale items")
            @RequestParam(defaultValue = "false") boolean onSaleOnly,
            @Parameter(description = "Sort field: relevance, price_asc, price_desc, newest, rating")
            @RequestParam(defaultValue = "relevance") String sortBy,
            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {

        ProductSearchResponse products = catalogService.searchProducts(
                query, categories, brands, minPrice, maxPrice, inStockOnly, onSaleOnly, sortBy, page, size
        );

        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/products/{slug}")
    @Operation(summary = "Get product by slug")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @Parameter(description = "Product slug")
            @PathVariable String slug) {

        ProductResponse product = catalogService.getProductBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/products/search/suggestions")
    @Operation(summary = "Get search suggestions")
    public ResponseEntity<ApiResponse<List<String>>> getSearchSuggestions(
            @Parameter(description = "Search query prefix")
            @RequestParam String query) {

        List<String> suggestions = catalogService.getSearchSuggestions(query);
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    @GetMapping("/products/similar/{slug}")
    @Operation(summary = "Get similar products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getSimilarProducts(
            @PathVariable String slug,
            @RequestParam(defaultValue = "6") int limit) {

        List<ProductResponse> products = catalogService.getSimilarProducts(slug, limit);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    // ===== Admin endpoints =====

    @PostMapping("/categories")
    @Operation(summary = "Create a new category (Admin)")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory() {
        return ResponseEntity.ok(null);
    }

    @PostMapping("/brands")
    @Operation(summary = "Create a new brand (Admin)")
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand() {
        return ResponseEntity.ok(null);
    }

    @PostMapping("/products")
    @Operation(summary = "Create a new product (Admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct() {
        return ResponseEntity.ok(null);
    }
}