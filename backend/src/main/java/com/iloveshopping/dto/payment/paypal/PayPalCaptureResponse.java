package com.iloveshopping.dto.payment.paypal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPalCaptureResponse {

    private String paypalOrderId;
    private String paymentId;
    private String status;
    private String orderId;
}
