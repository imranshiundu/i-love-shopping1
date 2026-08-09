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
public class ProductSearchRequest {

    private String query;
    private List<String> categoryIds;
    private List<String> brandIds;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean inStockOnly;
    private Boolean onSaleOnly;
    private String sortBy; // relevance, price_asc, price_desc, newest, rating
    private int page = 0;
    private int size = 20;
}