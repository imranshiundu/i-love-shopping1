package com.iloveshopping.controller;

import com.iloveshopping.dto.common.ApiResponse;
import com.iloveshopping.dto.order.CheckoutRequest;
import com.iloveshopping.dto.order.OrderResponse;
import com.iloveshopping.service.OrderService;
import com.iloveshopping.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management and checkout")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @PostMapping("/checkout")
    @Operation(summary = "Create order from current cart")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @CookieValue(name = "cartSessionId", required = false) String cookieSessionId,
            @RequestHeader(value = "X-Cart-Session", required = false) String headerSessionId) {
        String cartSessionId = cookieSessionId != null && !cookieSessionId.isBlank() ? cookieSessionId : headerSessionId;
        return ResponseEntity.ok(ApiResponse.success(orderService.checkout(request, cartSessionId)));
    }

    @GetMapping
    @Operation(summary = "List the current user's orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getUserOrders(page, size, status)));
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Get order by order number")
    public ResponseEntity<ApiResponse<OrderResponse>> get(
            @PathVariable String orderNumber,
            @CookieValue(name = "cartSessionId", required = false) String cookieSessionId,
            @RequestHeader(value = "X-Cart-Session", required = false) String headerSessionId) {
        String cartSessionId = cookieSessionId != null && !cookieSessionId.isBlank() ? cookieSessionId : headerSessionId;
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderByNumber(orderNumber, cartSessionId)));
    }

    @PostMapping("/{orderNumber}/cancel")
    @Operation(summary = "Cancel an order and restore stock + cart")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @PathVariable String orderNumber,
            @CookieValue(name = "cartSessionId", required = false) String cookieSessionId,
            @RequestHeader(value = "X-Cart-Session", required = false) String headerSessionId) {
        String cartSessionId = cookieSessionId != null && !cookieSessionId.isBlank() ? cookieSessionId : headerSessionId;
        return ResponseEntity.ok(ApiResponse.success(orderService.cancelOrder(orderNumber, cartSessionId)));
    }

    @DeleteMapping("/{orderNumber}")
    @Operation(summary = "Permanently delete an unpaid order (PENDING/EXPIRED/CANCELLED)")
    public ResponseEntity<ApiResponse<Void>> deleteUnpaid(
            @PathVariable String orderNumber,
            @CookieValue(name = "cartSessionId", required = false) String cookieSessionId,
            @RequestHeader(value = "X-Cart-Session", required = false) String headerSessionId) {
        String cartSessionId = cookieSessionId != null && !cookieSessionId.isBlank() ? cookieSessionId : headerSessionId;
        orderService.deleteUnpaidOrder(orderNumber, cartSessionId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ---- M-Pesa endpoints exposed here so paths stay under /orders/.../mpesa/... ----

    @PostMapping("/payments/mpesa/stk-push")
    @Operation(summary = "Initiate M-Pesa STK Push payment for an order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stkPush(@RequestBody Map<String, Object> body) {
        com.iloveshopping.dto.payment.MpesaStkPushRequest req = toStkRequest(body);
        com.iloveshopping.dto.payment.MpesaStkPushResponse res = paymentService.initiateMpesaStkPush(req);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "merchantRequestId", res.getMerchantRequestId(),
                "checkoutRequestId", res.getCheckoutRequestId(),
                "responseCode", res.getResponseCode(),
                "responseDescription", res.getResponseDescription() == null ? "" : res.getResponseDescription(),
                "customerMessage", res.getCustomerMessage() == null ? "" : res.getCustomerMessage()
        )));
    }

    @PostMapping("/payments/mpesa/callback")
    @Operation(summary = "M-Pesa Daraja callback")
    public ResponseEntity<Map<String, Object>> mpesaCallback(@RequestBody String body) {
        paymentService.processMpesaCallback(body);
        return ResponseEntity.ok(Map.of("ResultCode", 0, "ResultDesc", "Success"));
    }

    @PostMapping("/payments/mpesa/timeout")
    @Operation(summary = "M-Pesa Daraja timeout callback")
    public ResponseEntity<Map<String, Object>> mpesaTimeout(@RequestBody String body) {
        paymentService.processMpesaTimeout(body);
        return ResponseEntity.ok(Map.of("ResultCode", 0, "ResultDesc", "Success"));
    }

    @PostMapping("/payments/mpesa/stk-query")
    @Operation(summary = "Query M-Pesa STK Push status by checkoutRequestId")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stkQuery(@RequestBody Map<String, String> body) {
        String checkoutRequestId = body.get("checkoutRequestId");
        var res = paymentService.queryMpesaStkStatus(checkoutRequestId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "checkoutRequestId", res.getCheckoutRequestId(),
                "responseCode", res.getResponseCode(),
                "responseDescription", res.getResponseDescription() == null ? "" : res.getResponseDescription(),
                "customerMessage", res.getCustomerMessage() == null ? "" : res.getCustomerMessage()
        )));
    }

    @PostMapping("/{orderNumber}/retry-payment")
    @Operation(summary = "Retry M-Pesa payment for a PENDING/EXPIRED order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> retryPayment(
            @PathVariable String orderNumber,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) String phoneNumber) {
        // Accept phone from body OR query param for flexibility
        String phone = phoneNumber;
        if (phone == null && body != null) {
            Object p = body.get("phoneNumber");
            if (p != null) phone = p.toString();
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("phoneNumber is required");
        }
        var res = paymentService.retryMpesaPayment(orderNumber, phone);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "merchantRequestId", res.getMerchantRequestId(),
                "checkoutRequestId", res.getCheckoutRequestId(),
                "responseCode", res.getResponseCode(),
                "responseDescription", res.getResponseDescription() == null ? "" : res.getResponseDescription(),
                "customerMessage", res.getCustomerMessage() == null ? "" : res.getCustomerMessage()
        )));
    }

    private com.iloveshopping.dto.payment.MpesaStkPushRequest toStkRequest(Map<String, Object> body) {
        String orderId = String.valueOf(body.get("orderId"));
        Object amountRaw = body.get("amount");
        java.math.BigDecimal amount = amountRaw instanceof Number n
                ? new java.math.BigDecimal(n.toString())
                : new java.math.BigDecimal(String.valueOf(amountRaw));
        return com.iloveshopping.dto.payment.MpesaStkPushRequest.builder()
                .orderId(orderId)
                .amount(amount)
                .phoneNumber(String.valueOf(body.get("phoneNumber")))
                .accountReference(body.get("accountReference") == null ? null : String.valueOf(body.get("accountReference")))
                .transactionDesc(body.get("transactionDesc") == null ? null : String.valueOf(body.get("transactionDesc")))
                .build();
    }
}
