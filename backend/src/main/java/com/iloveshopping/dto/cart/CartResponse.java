package com.iloveshopping.dto.cart;

import com.iloveshopping.entity.Cart;
import com.iloveshopping.entity.CartItem;
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
public class CartResponse {

    private String id;
    private String userId;
    private String sessionId;
    private List<CartItemResponse> items;
    private int totalItems;
    private BigDecimal subtotal;

    public static CartResponse from(Cart cart) {
        if (cart == null) return null;
        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .sessionId(cart.getSessionId())
                .items(cart.getItems() != null ? cart.getItems().stream().map(CartItemResponse::from).toList() : List.of())
                .totalItems(cart.getTotalItems())
                .subtotal(cart.getSubtotal())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResponse {
        private String id;
        private String productId;
        private String productName;
        private String productSlug;
        private String productImage;
        private String variantId;
        private int quantity;
        private BigDecimal priceSnapshot;
        private BigDecimal lineTotal;
        private int maxStock;

        public static CartItemResponse from(CartItem item) {
            if (item == null) return null;
            return CartItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                    .productName(item.getProduct() != null ? item.getProduct().getName() : null)
                    .productSlug(item.getProduct() != null ? item.getProduct().getSlug() : null)
                    .productImage(item.getProduct() != null && item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()
                            ? item.getProduct().getImages().get(0).getUrl() : null)
                    .variantId(item.getVariantId())
                    .quantity(item.getQuantity())
                    .priceSnapshot(item.getPriceSnapshot())
                    .lineTotal(item.getLineTotal())
                    .maxStock(item.getProduct() != null ? item.getProduct().getStock() : 0)
                    .build();
        }
    }
}