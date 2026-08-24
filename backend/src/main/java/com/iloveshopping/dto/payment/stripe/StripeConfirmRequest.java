package com.iloveshopping.dto.payment.stripe;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeConfirmRequest {

    @NotBlank(message = "Payment intent ID is required")
    private String paymentIntentId;

    private String paymentMethodId;
}
