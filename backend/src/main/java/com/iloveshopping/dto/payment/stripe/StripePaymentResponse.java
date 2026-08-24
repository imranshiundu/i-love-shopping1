package com.iloveshopping.dto.payment.stripe;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripePaymentResponse {

    private String paymentIntentId;
    private String clientSecret;
    private String status;
    private String orderId;
    private String paymentId;
}
