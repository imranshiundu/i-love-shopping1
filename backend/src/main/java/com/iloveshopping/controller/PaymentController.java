package com.iloveshopping.controller;

import com.iloveshopping.config.StripeProperties;
import com.iloveshopping.dto.common.ApiResponse;
import com.iloveshopping.dto.payment.PaymentResponse;
import com.iloveshopping.service.FlutterwavePaymentService;
import com.iloveshopping.service.PaymentService;
import com.iloveshopping.service.StripePaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment history and provider lookups")
public class PaymentController {

    private final PaymentService paymentService;
    private final StripePaymentService stripePaymentService;
    private final FlutterwavePaymentService flutterwavePaymentService;
    private final StripeProperties stripeProperties;

    @GetMapping
    @Operation(summary = "List recent payments")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.listPayments(page, size).getContent()));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(@PathVariable String paymentId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentById(paymentId)));
    }

    @GetMapping("/mpesa/{checkoutRequestId}")
    @Operation(summary = "Get M-Pesa payment by CheckoutRequestID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByCheckoutRequestId(@PathVariable String checkoutRequestId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentByCheckoutRequestId(checkoutRequestId)));
    }

    // ---- Stripe ----

    @GetMapping("/stripe/config")
    @Operation(summary = "Get Stripe publishable key")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stripeConfig() {
        Map<String, Object> out = new HashMap<>();
        out.put("publishableKey", stripeProperties.getPublishableKey());
        out.put("configured", stripePaymentService.isConfigured());
        return ResponseEntity.ok(ApiResponse.success(out));
    }

    @PostMapping("/stripe/create-intent")
    @Operation(summary = "Create Stripe PaymentIntent for an order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createStripeIntent(@RequestBody Map<String, Object> body) {
        String orderId = String.valueOf(body.get("orderId"));
        Object amountRaw = body.get("amount");
        BigDecimal amount = amountRaw instanceof Number n
                ? new BigDecimal(n.toString())
                : new BigDecimal(String.valueOf(amountRaw));
        String currency = body.get("currency") == null ? "KES" : String.valueOf(body.get("currency"));
        return ResponseEntity.ok(ApiResponse.success(stripePaymentService.createPaymentIntent(orderId, amount, currency)));
    }

    @PostMapping("/stripe/confirm")
    @Operation(summary = "Confirm a Stripe payment intent")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmStripe(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                stripePaymentService.confirmPayment(body.get("paymentIntentId"))));
    }

    @PostMapping("/stripe/webhook")
    @Operation(summary = "Stripe webhook")
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sig) {
        try {
            stripePaymentService.processWebhook(payload, sig);
            return ResponseEntity.ok().build();
        } catch (com.stripe.exception.SignatureVerificationException e) {
            log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            // Check for signature verification failure (may be wrapped by @Transactional)
            Throwable cause = e;
            while (cause != null) {
                if (cause instanceof com.stripe.exception.SignatureVerificationException) {
                    log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
                    return ResponseEntity.badRequest().build();
                }
                cause = cause.getCause();
            }
            log.error("Stripe webhook error: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ---- Flutterwave ----

    @PostMapping("/flutterwave/initialize")
    @Operation(summary = "Initialize Flutterwave transaction for an order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initFlutterwave(@RequestBody Map<String, Object> body) {
        String orderId = String.valueOf(body.get("orderId"));
        Object amountRaw = body.get("amount");
        BigDecimal amount = amountRaw instanceof Number n
                ? new BigDecimal(n.toString())
                : new BigDecimal(String.valueOf(amountRaw));
        String currency = body.get("currency") == null ? "KES" : String.valueOf(body.get("currency"));
        String email = body.get("customerEmail") == null ? null : String.valueOf(body.get("customerEmail"));
        String name = body.get("customerName") == null ? null : String.valueOf(body.get("customerName"));
        return ResponseEntity.ok(ApiResponse.success(
                flutterwavePaymentService.initializeTransaction(orderId, amount, currency, email, name)));
    }

    @PostMapping("/flutterwave/verify")
    @Operation(summary = "Verify a Flutterwave transaction")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyFlutterwave(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                flutterwavePaymentService.verifyTransaction(body.get("transactionRef"))));
    }
}
