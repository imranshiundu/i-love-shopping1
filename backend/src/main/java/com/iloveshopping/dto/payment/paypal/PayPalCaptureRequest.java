package com.iloveshopping.dto.payment.paypal;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPalCaptureRequest {

    @NotBlank(message = "PayPal order ID is required")
    private String paypalOrderId;

    @NotBlank(message = "Payment ID is required")
    private String paymentId;
}
