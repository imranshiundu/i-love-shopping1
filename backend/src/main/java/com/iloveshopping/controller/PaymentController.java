package com.iloveshopping.controller;

import com.iloveshopping.dto.common.ApiResponse;
import com.iloveshopping.dto.payment.MpesaStkPushResponse;
import com.iloveshopping.dto.payment.PaymentResponse;
import com.iloveshopping.service.FlutterwavePaymentService;
import com.iloveshopping.service.MpesaService;
import com.iloveshopping.service.PaymentService;
import com.iloveshopping.service.StripePaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "M-Pesa, Stripe, and Flutterwave payment processing")
public class PaymentController {

    private final PaymentService paymentService;
    private final StripePaymentService stripePaymentService;
    private final FlutterwavePaymentService flutterwavePaymentService;

    @PostMapping("/mpesa/stk-query")
    @Operation(summary = "Query M-Pesa STK Push transaction status")
    public ResponseEntity<ApiResponse<MpesaStkPushResponse>> queryStkStatus(
            @RequestBody Map<String, String> request) {

        MpesaStkPushResponse response = paymentService.queryStkStatus(request.get("checkoutRequestId"));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/mpesa/{checkoutRequestId}")
    @Operation(summary = "Get payment by M-Pesa checkout request ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getMpesaPayment(
            @PathVariable String checkoutRequestId) {

        PaymentResponse response = paymentService.getPaymentByCheckoutRequestId(checkoutRequestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/stripe/create-intent")
    @Operation(summary = "Create a Stripe PaymentIntent")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createStripeIntent(
            @RequestBody Map<String, Object> request) {

        String orderId = (String) request.get("orderId");
        BigDecimal amount = new BigDecimal(String.valueOf(request.get("amount")));
        String currency = (String) request.getOrDefault("currency", "KES");

        Map<String, Object> response = stripePaymentService.createPaymentIntent(orderId, amount, currency);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/stripe/confirm")
    @Operation(summary = "Confirm a Stripe payment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmStripePayment(
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = stripePaymentService.confirmPayment(request.get("paymentIntentId"));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/stripe/webhook")
    @Operation(summary = "Stripe webhook endpoint")
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        stripePaymentService.processWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stripe/config")
    @Operation(summary = "Get Stripe publishable key (safe for client)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStripeConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("publishableKey", stripePaymentService.isConfigured()
                ? stripePaymentService.getClass().getPackageName() : null);
        config.put("configured", stripePaymentService.isConfigured());
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @PostMapping("/flutterwave/initialize")
    @Operation(summary = "Initialize a Flutterwave transaction")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initializeFlutterwave(
            @RequestBody Map<String, Object> request) {

        String orderId = (String) request.get("orderId");
        BigDecimal amount = new BigDecimal(String.valueOf(request.get("amount")));
        String currency = (String) request.getOrDefault("currency", "KES");
        String email = (String) request.get("customerEmail");
        String name = (String) request.get("customerName");

        Map<String, Object> response = flutterwavePaymentService.initializeTransaction(orderId, amount, currency, email, name);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/flutterwave/verify")
    @Operation(summary = "Verify a Flutterwave transaction")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyFlutterwave(
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = flutterwavePaymentService.verifyTransaction(request.get("transactionRef"));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get current user's payment history")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<PaymentResponse> payments = paymentService.getUserPayments(page, size);
        return ResponseEntity.ok(ApiResponse.success(payments.getContent()));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable String paymentId) {

        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
