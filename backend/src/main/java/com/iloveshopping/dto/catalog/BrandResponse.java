package com.iloveshopping.dto.catalog;

import com.iloveshopping.entity.Brand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandResponse {

    private String id;
    private String name;
    private String slug;
    private String logo;
    private String description;
    private long productCount;

    public static BrandResponse from(Brand brand) {
        if (brand == null) return null;
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .slug(brand.getSlug())
                .logo(brand.getLogo())
                .description(brand.getDescription())
                .productCount(brand.getProducts() != null ? brand.getProducts().size() : 0)
                .build();
    }
}