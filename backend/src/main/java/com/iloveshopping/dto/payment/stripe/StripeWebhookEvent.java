package com.iloveshopping.dto.payment.stripe;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeWebhookEvent {

    private String type;
    private String paymentIntentId;
    private String status;
    private String orderId;
}
