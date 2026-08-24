package com.iloveshopping.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent implements Serializable {

    private String orderId;
    private String orderNumber;
    private String userId;
    private String userEmail;
    private String eventName;
    private String orderStatus;
    private BigDecimal totalAmount;
    private String currency;
    private int itemCount;
    private LocalDateTime timestamp;

    public enum EventType {
        ORDER_CREATED, ORDER_PAID, ORDER_CANCELLED
    }
}
