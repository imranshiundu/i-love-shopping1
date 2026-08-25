package com.iloveshopping.dto.order;

import com.iloveshopping.entity.Order;
import com.iloveshopping.entity.OrderItem;
import com.iloveshopping.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private String id;
    private String number;
    private String userId;
    private Order.OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shipping;
    private BigDecimal total;
    private String currency;
    private AddressDto shippingAddress;
    private AddressDto billingAddress;
    private String notes;
    private List<OrderItemResponse> items;
    private List<PaymentResponse> payments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal totalPaid;
    private boolean fullyPaid;
    private boolean canBeCancelled;

    public static OrderResponse from(Order order) {
        if (order == null) return null;
        return OrderResponse.builder()
                .id(order.getId())
                .number(order.getNumber())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .tax(order.getTax())
                .shipping(order.getShipping())
                .total(order.getTotal())
                .currency(order.getCurrency())
                .shippingAddress(order.getShippingAddress() != null ? AddressDto.fromJson(order.getShippingAddress()) : null)
                .billingAddress(order.getBillingAddress() != null ? AddressDto.fromJson(order.getBillingAddress()) : null)
                .notes(order.getNotes())
                .items(order.getItems() != null ? order.getItems().stream().map(OrderItemResponse::from).toList() : List.of())
                .payments(order.getPayments() != null ? order.getPayments().stream().map(PaymentResponse::from).toList() : List.of())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .totalPaid(order.getTotalPaid())
                .fullyPaid(order.isFullyPaid())
                .canBeCancelled(order.canBeCancelled())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private String id;
        private String productId;
        private String productName;
        private String productImage;
        private String variantId;
        private int quantity;
        private BigDecimal price;
        private BigDecimal total;

        private static String extractFirstImage(OrderItem item) {
            try {
                if (item.getProduct() != null && item.getProduct().getImages() != null
                        && !item.getProduct().getImages().isEmpty()) {
                    return item.getProduct().getImages().get(0).getUrl();
                }
            } catch (Exception ignored) {
            }
            return null;
        }

        public static OrderItemResponse from(OrderItem item) {
            if (item == null) return null;
            return OrderItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                    .productName(item.getName())
                    .productImage(extractFirstImage(item))
                    .variantId(item.getVariantId())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .total(item.getTotal())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentResponse {
        private String id;
        private String provider;
        private String providerId;
        private BigDecimal amount;
        private String currency;
        private String status;
        private LocalDateTime createdAt;

        public static PaymentResponse from(Payment payment) {
            if (payment == null) return null;
            return PaymentResponse.builder()
                    .id(payment.getId())
                    .provider(payment.getProvider().name())
                    .providerId(payment.getProviderId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .status(payment.getStatus().name())
                    .createdAt(payment.getCreatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDto {
        private String name;
        private String line1;
        private String line2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private String phone;

        public static AddressDto fromJson(String json) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(json, AddressDto.class);
            } catch (Exception e) {
                return null;
            }
        }
    }
}