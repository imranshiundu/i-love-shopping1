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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    // ===== M-Pesa Sandbox Simulation =====

    @PostMapping("/mpesa/simulate-confirm")
    @Operation(summary = "Simulate customer approving the M-Pesa PIN prompt (sandbox)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> simulateMpesaConfirm(
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = paymentSimulationService.completeSimulatedMpesaPayment(request.get("checkoutRequestId"));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ===== Flutterwave Endpoints =====

    @PostMapping("/flutterwave/create-payment")
    @Operation(summary = "Create a simulated Flutterwave payment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createFlutterwavePayment(
            @RequestBody Map<String, Object> request) {

        String orderId = (String) request.get("orderId");
        BigDecimal amount = new BigDecimal(String.valueOf(request.get("amount")));
        String currency = (String) request.getOrDefault("currency", "KES");
        String email = (String) request.get("customerEmail");

        Map<String, Object> response = paymentSimulationService.createFlutterwavePayment(orderId, amount, currency, email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/flutterwave/verify")
    @Operation(summary = "Verify a simulated Flutterwave payment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyFlutterwavePayment(
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = paymentSimulationService.verifyFlutterwavePayment(request.get("transactionRef"));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ===== Airtel Money Endpoints =====

    @PostMapping("/airtel/initiate")
    @Operation(summary = "Initiate a simulated Airtel Money payment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiateAirtelMoney(
            @RequestBody Map<String, Object> request) {

        String orderId = (String) request.get("orderId");
        BigDecimal amount = new BigDecimal(String.valueOf(request.get("amount")));
        String phoneNumber = (String) request.get("phoneNumber");

        Map<String, Object> response = paymentSimulationService.initiateAirtelMoney(orderId, amount, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/airtel/confirm")
    @Operation(summary = "Confirm a simulated Airtel Money PIN entry")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmAirtelMoney(
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = paymentSimulationService.confirmAirtelMoney(request.get("referenceId"));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
