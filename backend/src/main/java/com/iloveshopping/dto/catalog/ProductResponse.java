package com.iloveshopping.dto.catalog;

import com.iloveshopping.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private String id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private String sku;
    private int stock;
    private BigDecimal weight;
    private String weightUnit;
    private String dimensions;
    private boolean isActive;
    private CategorySummary category;
    private BrandSummary brand;
    private List<ProductImageResponse> images;
    private boolean inStock;
    private boolean onSale;
    private int discountPercentage;
    private double averageRating;
    private long reviewCount;

    public static ProductResponse from(Product product) {
        if (product == null) return null;
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .compareAtPrice(product.getCompareAtPrice())
                .sku(product.getSku())
                .stock(product.getStock())
                .weight(product.getWeight())
                .weightUnit(product.getWeightUnit())
                .dimensions(product.getDimensions())
                .isActive(product.getIsActive())
                .category(product.getCategory() != null ? CategorySummary.from(product.getCategory()) : null)
                .brand(product.getBrand() != null ? BrandSummary.from(product.getBrand()) : null)
                .images(product.getImages() != null ? product.getImages().stream().map(ProductImageResponse::from).toList() : List.of())
                .inStock(product.isInStock())
                .onSale(product.isOnSale())
                .discountPercentage(product.getDiscountPercentage())
                .averageRating(product.getAverageRating() != null ? product.getAverageRating() : 0.0)
                .reviewCount(product.getReviewCount() != null ? product.getReviewCount() : 0L)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private String id;
        private String name;
        private String slug;

        public static CategorySummary from(com.iloveshopping.entity.Category category) {
            if (category == null) return null;
            return CategorySummary.builder()
                    .id(category.getId())
                    .name(category.getName())
                    .slug(category.getSlug())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrandSummary {
        private String id;
        private String name;
        private String slug;

        public static BrandSummary from(com.iloveshopping.entity.Brand brand) {
            if (brand == null) return null;
            return BrandSummary.builder()
                    .id(brand.getId())
                    .name(brand.getName())
                    .slug(brand.getSlug())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductImageResponse {
        private String id;
        private String url;
        private String alt;
        private int sortOrder;

        public static ProductImageResponse from(com.iloveshopping.entity.ProductImage image) {
            if (image == null) return null;
            return ProductImageResponse.builder()
                    .id(image.getId())
                    .url(image.getUrl())
                    .alt(image.getAlt())
                    .sortOrder(image.getSortOrder())
                    .build();
        }
    }
}