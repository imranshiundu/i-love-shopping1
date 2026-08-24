package com.iloveshopping.dto.payment.stripe;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeCreateIntentRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least KES 1.00")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    private String paymentMethodId;
    private String description;
    private String customerEmail;
}
