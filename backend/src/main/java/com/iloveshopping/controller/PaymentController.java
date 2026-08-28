package com.iloveshopping.controller;

import com.iloveshopping.dto.common.ApiResponse;
import com.iloveshopping.dto.payment.MpesaStkPushResponse;
import com.iloveshopping.dto.payment.PaymentResponse;
import com.iloveshopping.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "M-Pesa payment processing and transaction history")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/mpesa/stk-query")
    @Operation(summary = "Query M-Pesa STK Push transaction status")
    public ResponseEntity<ApiResponse<MpesaStkPushResponse>> queryStkStatus(
            @RequestBody java.util.Map<String, String> request) {

        MpesaStkPushResponse response = paymentService.queryStkStatus(request.get("checkoutRequestId"));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/mpesa/{checkoutRequestId}")
    @Operation(summary = "Get payment by M-Pesa checkout request ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable String checkoutRequestId) {

        PaymentResponse response = paymentService.getPaymentByCheckoutRequestId(checkoutRequestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get current user's payment history")
    public ResponseEntity<ApiResponse<java.util.List<PaymentResponse>>> getPayments(
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
