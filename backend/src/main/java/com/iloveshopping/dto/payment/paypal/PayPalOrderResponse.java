package com.iloveshopping.dto.payment.paypal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPalOrderResponse {

    private String paypalOrderId;
    private String status;
    private String orderId;
    private String paymentId;
    private BigDecimal amount;
    private String currency;
    private String approveUrl;
}
