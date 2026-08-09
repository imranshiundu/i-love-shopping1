package com.iloveshopping.controller;

import com.iloveshopping.dto.common.ApiResponse;
import com.iloveshopping.dto.order.CheckoutRequest;
import com.iloveshopping.dto.order.OrderResponse;
import com.iloveshopping.dto.payment.MpesaStkPushRequest;
import com.iloveshopping.dto.payment.MpesaStkPushResponse;
import com.iloveshopping.dto.payment.PaymentResponse;
import com.iloveshopping.entity.Order;
import com.iloveshopping.service.OrderService;
import com.iloveshopping.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management and checkout")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @PostMapping("/checkout")
    @Operation(summary = "Create order from current cart (checkout)")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @Valid @RequestBody CheckoutRequest request) {

        OrderResponse order = orderService.checkout(request);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping
    @Operation(summary = "Get current user's orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<OrderResponse> orders = orderService.getUserOrders(page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Get order by number")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable String orderNumber) {

        OrderResponse order = orderService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/{orderNumber}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable String orderNumber) {

        OrderResponse order = orderService.cancelOrder(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    // ===== Payment endpoints =====

    @PostMapping("/payments/mpesa/stk-push")
    @Operation(summary = "Initiate M-Pesa STK Push payment")
    public ResponseEntity<ApiResponse<MpesaStkPushResponse>> initiateMpesaPayment(
            @Valid @RequestBody MpesaStkPushRequest request) {

        MpesaStkPushResponse response = paymentService.initiateStkPush(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/payments/mpesa/callback")
    @Operation(summary = "M-Pesa payment callback (Daraja)")
    public ResponseEntity<Void> mpesaCallback(
            @RequestBody String callbackBody) {

        paymentService.processMpesaCallback(callbackBody);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/payments/mpesa/timeout")
    @Operation(summary = "M-Pesa payment timeout callback")
    public ResponseEntity<Void> mpesaTimeout(@RequestBody String callbackBody) {
        paymentService.processMpesaTimeout(callbackBody);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{orderNumber}/payments")
    @Operation(summary = "Get payments for an order")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getOrderPayments(
            @PathVariable String orderNumber) {

        List<PaymentResponse> payments = paymentService.getOrderPayments(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @PostMapping("/{orderNumber}/retry-payment")
    @Operation(summary = "Retry payment for an order")
    public ResponseEntity<ApiResponse<MpesaStkPushResponse>> retryPayment(
            @PathVariable String orderNumber) {

        MpesaStkPushResponse response = paymentService.retryPayment(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}