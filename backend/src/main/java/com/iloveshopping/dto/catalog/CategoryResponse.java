package com.iloveshopping.dto.catalog;

import com.iloveshopping.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private String id;
    private String name;
    private String slug;
    private String description;
    private String image;
    private int sortOrder;
    private String parentId;
    private List<CategoryResponse> children;
    private long productCount;

    public static CategoryResponse from(Category category) {
        if (category == null) return null;
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .image(category.getImage())
                .sortOrder(category.getSortOrder())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .children(category.getChildren() != null ? category.getChildren().stream().map(CategoryResponse::from).toList() : List.of())
                .productCount(category.getProducts() != null ? category.getProducts().size() : 0)
                .build();
    }
}