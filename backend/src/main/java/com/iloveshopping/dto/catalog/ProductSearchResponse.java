package com.iloveshopping.dto.catalog;

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
public class ProductSearchResponse {

    private List<ProductResponse> products;
    private PageInfo pagination;
    private FacetCounts facets;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacetCounts {
        private List<CategoryFacet> categories;
        private List<BrandFacet> brands;
        private PriceRangeFacet priceRange;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CategoryFacet {
            private String categoryId;
            private String categoryName;
            private long count;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class BrandFacet {
            private String brandId;
            private String brandName;
            private long count;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PriceRangeFacet {
            private BigDecimal min;
            private BigDecimal max;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }
}