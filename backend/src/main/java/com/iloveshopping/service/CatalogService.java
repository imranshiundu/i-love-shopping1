package com.iloveshopping.service;

import com.iloveshopping.dto.catalog.BrandResponse;
import com.iloveshopping.dto.catalog.CategoryResponse;
import com.iloveshopping.dto.catalog.ProductResponse;
import com.iloveshopping.dto.catalog.ProductSearchResponse;
import com.iloveshopping.entity.Brand;
import com.iloveshopping.entity.Category;
import com.iloveshopping.entity.Product;
import com.iloveshopping.exception.ResourceNotFoundException;
import com.iloveshopping.repository.BrandRepository;
import com.iloveshopping.repository.CategoryRepository;
import com.iloveshopping.repository.ProductRepository;
import com.iloveshopping.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PRODUCT_CACHE_PREFIX = "product:";
    private static final String CATEGORY_CACHE_PREFIX = "category:";
    private static final String BRAND_CACHE_PREFIX = "brand:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    // ===== Categories =====

    public List<CategoryResponse> getAllCategories(boolean includeChildren) {
        String cacheKey = CATEGORY_CACHE_PREFIX + "all:" + includeChildren;
        List<CategoryResponse> cached = (List<CategoryResponse>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<Category> categories = categoryRepository.findByParentIdIsNullOrderBySortOrderAscNameAsc();
        List<CategoryResponse> responses = categories.stream()
                .map(cat -> toCategoryResponse(cat, includeChildren))
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(cacheKey, responses, CACHE_TTL);
        return responses;
    }

    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));

        return toCategoryResponse(category, true);
    }

    public Page<ProductResponse> getCategoryProducts(String slug, int page, int size, String sortBy) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));

        Sort sort = getSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> products = productRepository.findByCategoryId(category.getId(), pageable);
        return products.map(this::toProductResponseWithDetails);
    }

    // ===== Brands =====

    public List<BrandResponse> getAllBrands() {
        String cacheKey = BRAND_CACHE_PREFIX + "all";
        List<BrandResponse> cached = (List<BrandResponse>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<Brand> brands = brandRepository.findAllByOrderByNameAsc();
        List<BrandResponse> responses = brands.stream()
                .map(BrandResponse::from)
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(cacheKey, responses, CACHE_TTL);
        return responses;
    }

    public BrandResponse getBrandBySlug(String slug) {
        Brand brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", "slug", slug));

        return BrandResponse.from(brand);
    }

    public Page<ProductResponse> getBrandProducts(String slug, int page, int size, String sortBy) {
        Brand brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", "slug", slug));

        Sort sort = getSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> products = productRepository.findByBrandId(brand.getId(), pageable);
        return products.map(this::toProductResponseWithDetails);
    }

    // ===== Products =====

    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));

        return toProductResponseWithDetails(product);
    }

    public ProductSearchResponse searchProducts(String query, List<String> categorySlugs,
                                                 List<String> brandSlugs, BigDecimal minPrice,
                                                 BigDecimal maxPrice, boolean inStockOnly,
                                                 boolean onSaleOnly, String sortBy,
                                                 int page, int size) {

        List<Product> allProducts = productRepository.findAllActive();

        List<Product> filtered = allProducts.stream()
                .filter(p -> query == null || query.isBlank() ||
                        p.getName().toLowerCase().contains(query.toLowerCase()))
                .filter(p -> categorySlugs == null || categorySlugs.isEmpty() ||
                        categorySlugs.contains(p.getCategory().getSlug()) ||
                        (p.getCategory().getParent() != null && categorySlugs.contains(p.getCategory().getParent().getSlug())))
                .filter(p -> brandSlugs == null || brandSlugs.isEmpty() ||
                        brandSlugs.contains(p.getBrand().getSlug()))
                .filter(p -> minPrice == null || p.getPrice().compareTo(minPrice) >= 0)
                .filter(p -> maxPrice == null || p.getPrice().compareTo(maxPrice) <= 0)
                .filter(p -> !inStockOnly || p.getStock() > 0)
                .filter(p -> !onSaleOnly || p.isOnSale())
                .sorted(getProductComparator(sortBy, query))
                .collect(Collectors.toList());

        int totalElements = filtered.size();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<Product> pageContent = filtered.subList(fromIndex, toIndex);

        List<ProductResponse> products = pageContent.stream()
                .map(this::toProductResponseWithDetails)
                .collect(Collectors.toList());

        ProductSearchResponse.PageInfo pageInfo = ProductSearchResponse.PageInfo.builder()
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages((int) Math.ceil(totalElements / (double) size))
                .hasNext(toIndex < totalElements)
                .hasPrevious(fromIndex > 0)
                .build();

        return ProductSearchResponse.builder()
                .products(products)
                .pagination(pageInfo)
                .facets(getFacets(query, categorySlugs, brandSlugs, minPrice, maxPrice))
                .build();
    }

    public List<String> getSearchSuggestions(String query) {
        if (query == null || query.length() < 2) {
            return List.of();
        }

        List<String> suggestions = new java.util.ArrayList<>();
        for (int i = Math.min(query.length(), 5); i >= 2; i--) {
            String prefix = query.substring(0, i).toLowerCase();
            List<String> productNames = productRepository.findTopByNameStartsWithIgnoreCase(prefix, PageRequest.of(0, 10));
            suggestions.addAll(productNames);
        }

        return suggestions.stream().distinct().limit(10).collect(Collectors.toList());
    }

    public List<ProductResponse> getSimilarProducts(String slug, int limit) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));

        List<Product> similar = productRepository.findSimilarProducts(
                product.getId(), product.getCategory().getId(), product.getBrand().getId(),
                PageRequest.of(0, limit)
        );

        return similar.stream()
                .map(this::toProductResponseWithDetails)
                .collect(Collectors.toList());
    }

    // ===== Helpers =====

    private CategoryResponse toCategoryResponse(Category category, boolean includeChildren) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .image(category.getImage())
                .sortOrder(category.getSortOrder())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .children(includeChildren && category.getChildren() != null
                        ? category.getChildren().stream().map(c -> toCategoryResponse(c, false)).collect(Collectors.toList())
                        : List.of())
                .productCount(category.getProducts() != null ? category.getProducts().size() : 0)
                .build();
    }

    private ProductResponse toProductResponseWithDetails(Product product) {
        return ProductResponse.from(product);
    }

    private ProductSearchResponse.FacetCounts getFacets(String query, List<String> categorySlugs,
                                                        List<String> brandSlugs, BigDecimal minPrice,
                                                        BigDecimal maxPrice) {
        return ProductSearchResponse.FacetCounts.builder()
                .categories(List.of())
                .brands(List.of())
                .priceRange(ProductSearchResponse.FacetCounts.PriceRangeFacet.builder()
                        .min(BigDecimal.ZERO)
                        .max(BigDecimal.valueOf(10000))
                        .build())
                .build();
    }

    private Sort getSort(String sortBy) {
        return switch (sortBy != null ? sortBy.toLowerCase() : "relevance") {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "rating" -> Sort.by(Sort.Direction.DESC, "averageRating");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private java.util.Comparator<Product> getProductComparator(String sortBy, String query) {
        if ("rating".equalsIgnoreCase(sortBy)) {
            return java.util.Comparator.comparing((Product p) -> p.getAverageRating() != null ? p.getAverageRating() : 0.0).reversed();
        }
        if ("relevance".equalsIgnoreCase(sortBy) && query != null && !query.isBlank()) {
            return (p1, p2) -> {
                String q = query.toLowerCase();
                int score1 = (p1.getName().toLowerCase().contains(q) ? 2 : 0) + (p1.getDescription().toLowerCase().contains(q) ? 1 : 0);
                int score2 = (p2.getName().toLowerCase().contains(q) ? 2 : 0) + (p2.getDescription().toLowerCase().contains(q) ? 1 : 0);
                return Integer.compare(score2, score1);
            };
        }
        return switch (sortBy != null ? sortBy.toLowerCase() : "relevance") {
            case "price_asc" -> java.util.Comparator.comparing(Product::getPrice);
            case "price_desc" -> java.util.Comparator.comparing(Product::getPrice).reversed();
            case "newest" -> java.util.Comparator.comparing(Product::getCreatedAt).reversed();
            default -> java.util.Comparator.comparing(Product::getCreatedAt).reversed();
        };
    }
}