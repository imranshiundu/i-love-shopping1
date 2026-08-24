package com.iloveshopping.controller;

import com.iloveshopping.dto.common.ApiResponse;
import com.iloveshopping.dto.payment.paypal.PayPalCaptureRequest;
import com.iloveshopping.dto.payment.paypal.PayPalCaptureResponse;
import com.iloveshopping.dto.payment.paypal.PayPalCreateOrderRequest;
import com.iloveshopping.dto.payment.paypal.PayPalOrderResponse;
import com.iloveshopping.dto.payment.stripe.*;
import com.iloveshopping.service.PaymentSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Simulation", description = "Stripe and PayPal payment simulation (sandbox mode)")
public class PaymentSimulationController {

    private final PaymentSimulationService paymentSimulationService;

    // ===== Stripe Endpoints =====

    @PostMapping("/stripe/create-intent")
    @Operation(summary = "Create a simulated Stripe payment intent")
    public ResponseEntity<ApiResponse<StripePaymentResponse>> createStripeIntent(
            @Valid @RequestBody StripeCreateIntentRequest request) {
        StripePaymentResponse response = paymentSimulationService.createStripePaymentIntent(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/stripe/confirm")
    @Operation(summary = "Confirm a simulated Stripe payment")
    public ResponseEntity<ApiResponse<StripePaymentResponse>> confirmStripePayment(
            @Valid @RequestBody StripeConfirmRequest request) {
        StripePaymentResponse response = paymentSimulationService.confirmStripePayment(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/stripe/webhook")
    @Operation(summary = "Simulate a Stripe webhook event")
    public ResponseEntity<Void> stripeWebhook(@RequestBody StripeWebhookEvent event) {
        paymentSimulationService.processStripeWebhook(event);
        return ResponseEntity.ok().build();
    }

    // ===== PayPal Endpoints =====

    @PostMapping("/paypal/create-order")
    @Operation(summary = "Create a simulated PayPal order")
    public ResponseEntity<ApiResponse<PayPalOrderResponse>> createPayPalOrder(
            @Valid @RequestBody PayPalCreateOrderRequest request) {
        PayPalOrderResponse response = paymentSimulationService.createPayPalOrder(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/paypal/capture")
    @Operation(summary = "Capture a simulated PayPal payment")
    public ResponseEntity<ApiResponse<PayPalCaptureResponse>> capturePayPalPayment(
            @Valid @RequestBody PayPalCaptureRequest request) {
        PayPalCaptureResponse response = paymentSimulationService.capturePayPalPayment(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
