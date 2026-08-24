package com.iloveshopping.service;

import com.iloveshopping.dto.payment.paypal.PayPalCaptureRequest;
import com.iloveshopping.dto.payment.paypal.PayPalCaptureResponse;
import com.iloveshopping.dto.payment.paypal.PayPalCreateOrderRequest;
import com.iloveshopping.dto.payment.paypal.PayPalOrderResponse;
import com.iloveshopping.dto.payment.stripe.*;
import com.iloveshopping.entity.Order;
import com.iloveshopping.entity.Payment;
import com.iloveshopping.exception.ApiException;
import com.iloveshopping.messaging.OrderMessagePublisher;
import com.iloveshopping.repository.OrderRepository;
import com.iloveshopping.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentSimulationService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final OrderMessagePublisher orderMessagePublisher;
    private final EmailService emailService;

    // ===== Stripe Simulation =====

    public StripePaymentResponse createStripePaymentIntent(StripeCreateIntentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> ApiException.notFound("Order not found: " + request.getOrderId()));

        String paymentIntentId = "pi_sim_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String clientSecret = paymentIntentId + "_secret_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        Payment payment = Payment.builder()
                .order(order)
                .provider(Payment.PaymentProvider.STRIPE)
                .providerId(paymentIntentId)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "KES")
                .status(Payment.PaymentStatus.PENDING)
                .metadata(buildStripeMetadata(request))
                .build();

        payment = paymentRepository.save(payment);

        log.info("Created simulated Stripe payment intent: {} for order: {}", paymentIntentId, order.getNumber());

        return StripePaymentResponse.builder()
                .paymentIntentId(paymentIntentId)
                .clientSecret(clientSecret)
                .status("requires_payment_method")
                .orderId(order.getId())
                .paymentId(payment.getId())
                .build();
    }

    public StripePaymentResponse confirmStripePayment(StripeConfirmRequest request) {
        Payment payment = paymentRepository.findByProviderId(request.getPaymentIntentId())
                .orElseThrow(() -> ApiException.notFound("Payment not found: " + request.getPaymentIntentId()));

        if (payment.getProvider() != Payment.PaymentProvider.STRIPE) {
            throw ApiException.badRequest("Payment is not a Stripe payment");
        }

        if (payment.getStatus() != Payment.PaymentStatus.PENDING &&
            payment.getStatus() != Payment.PaymentStatus.PROCESSING) {
            throw ApiException.badRequest("Payment cannot be confirmed in current status: " + payment.getStatus());
        }

        payment.setStatus(Payment.PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        // Simulate: 95% success rate for confirmed payments
        boolean success = Math.random() < 0.95;

        if (success) {
            payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
            payment.setCallbackData(buildStripeConfirmCallbackData("succeeded"));
            paymentRepository.save(payment);

            onPaymentSucceeded(payment);
            log.info("Simulated Stripe payment succeeded: {}", request.getPaymentIntentId());
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setCallbackData(buildStripeConfirmCallbackData("failed"));
            paymentRepository.save(payment);

            onPaymentFailed(payment);
            log.info("Simulated Stripe payment failed: {}", request.getPaymentIntentId());
        }

        return StripePaymentResponse.builder()
                .paymentIntentId(payment.getProviderId())
                .clientSecret(null)
                .status(payment.getStatus().name().toLowerCase())
                .orderId(payment.getOrder().getId())
                .paymentId(payment.getId())
                .build();
    }

    public void processStripeWebhook(StripeWebhookEvent event) {
        log.info("Processing simulated Stripe webhook: type={}, paymentIntentId={}", event.getType(), event.getPaymentIntentId());

        Payment payment = paymentRepository.findByProviderId(event.getPaymentIntentId())
                .orElseThrow(() -> ApiException.notFound("Payment not found: " + event.getPaymentIntentId()));

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
                payment.setCallbackData(buildStripeWebhookCallbackData(event));
                paymentRepository.save(payment);
                onPaymentSucceeded(payment);
                log.info("Stripe webhook: payment succeeded for {}", event.getPaymentIntentId());
            }
            case "payment_intent.payment_failed" -> {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setCallbackData(buildStripeWebhookCallbackData(event));
                paymentRepository.save(payment);
                onPaymentFailed(payment);
                log.info("Stripe webhook: payment failed for {}", event.getPaymentIntentId());
            }
            case "payment_intent.processing" -> {
                payment.setStatus(Payment.PaymentStatus.PROCESSING);
                payment.setCallbackData(buildStripeWebhookCallbackData(event));
                paymentRepository.save(payment);
                log.info("Stripe webhook: payment processing for {}", event.getPaymentIntentId());
            }
            default -> log.warn("Unhandled Stripe webhook type: {}", event.getType());
        }
    }

    // ===== PayPal Simulation =====

    public PayPalOrderResponse createPayPalOrder(PayPalCreateOrderRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> ApiException.notFound("Order not found: " + request.getOrderId()));

        String paypalOrderId = "PAYID-L_SIM_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String paymentId = "sim_paypal_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        Payment payment = Payment.builder()
                .order(order)
                .provider(Payment.PaymentProvider.PAYPAL)
                .providerId(paypalOrderId)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "KES")
                .status(Payment.PaymentStatus.PENDING)
                .metadata(buildPayPalMetadata(request))
                .build();

        payment = paymentRepository.save(payment);

        String approveUrl = "https://www.sandbox.paypal.com/checkoutnow?token=" + paypalOrderId;

        log.info("Created simulated PayPal order: {} for order: {}", paypalOrderId, order.getNumber());

        return PayPalOrderResponse.builder()
                .paypalOrderId(paypalOrderId)
                .status("CREATED")
                .orderId(order.getId())
                .paymentId(payment.getId())
                .amount(request.getAmount())
                .currency(payment.getCurrency())
                .approveUrl(approveUrl)
                .build();
    }

    public PayPalCaptureResponse capturePayPalPayment(PayPalCaptureRequest request) {
        Payment payment = paymentRepository.findByProviderId(request.getPaypalOrderId())
                .orElseThrow(() -> ApiException.notFound("Payment not found: " + request.getPaypalOrderId()));

        if (payment.getProvider() != Payment.PaymentProvider.PAYPAL) {
            throw ApiException.badRequest("Payment is not a PayPal payment");
        }

        if (payment.getStatus() != Payment.PaymentStatus.PENDING &&
            payment.getStatus() != Payment.PaymentStatus.PROCESSING) {
            throw ApiException.badRequest("Payment cannot be captured in current status: " + payment.getStatus());
        }

        payment.setStatus(Payment.PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        // Simulate: 97% success rate for captures
        boolean success = Math.random() < 0.97;

        if (success) {
            payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
            payment.setCallbackData(buildPayPalCaptureCallbackData("succeeded"));
            paymentRepository.save(payment);

            onPaymentSucceeded(payment);
            log.info("Simulated PayPal payment captured: {}", request.getPaypalOrderId());
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setCallbackData(buildPayPalCaptureCallbackData("failed"));
            paymentRepository.save(payment);

            onPaymentFailed(payment);
            log.info("Simulated PayPal payment capture failed: {}", request.getPaypalOrderId());
        }

        return PayPalCaptureResponse.builder()
                .paypalOrderId(payment.getProviderId())
                .paymentId(payment.getId())
                .status(payment.getStatus().name().toLowerCase())
                .orderId(payment.getOrder().getId())
                .build();
    }

    // ===== Payment outcome handlers =====

    private void onPaymentSucceeded(Payment payment) {
        Order order = payment.getOrder();
        try {
            orderMessagePublisher.publishOrderPaid(order);
        } catch (Exception e) {
            log.error("Failed to publish ORDER_PAID event for order {}: {}", order.getNumber(), e.getMessage());
        }
        if (order.getUser() != null && order.getUser().getEmail() != null) {
            try {
                emailService.sendOrderConfirmation(order.getUser().getEmail(), order.getNumber(), order.getId().toString());
            } catch (Exception e) {
                log.error("Failed to send order confirmation email for {}: {}", order.getNumber(), e.getMessage());
            }
        }
    }

    private void onPaymentFailed(Payment payment) {
        Order order = payment.getOrder();
        log.warn("Payment failed for order {} - order remains {} pending retry or cancellation",
                order.getNumber(), order.getStatus());
    }

    // ===== Metadata builders =====

    private String buildStripeMetadata(StripeCreateIntentRequest request) {
        try {
            return objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                put("paymentMethodId", request.getPaymentMethodId());
                put("description", request.getDescription());
                put("customerEmail", request.getCustomerEmail());
                put("simulated", true);
            }});
        } catch (Exception e) {
            return "{\"simulated\":true}";
        }
    }

    private String buildPayPalMetadata(PayPalCreateOrderRequest request) {
        try {
            return objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                put("description", request.getDescription());
                put("customerEmail", request.getCustomerEmail());
                put("simulated", true);
            }});
        } catch (Exception e) {
            return "{\"simulated\":true}";
        }
    }

    private String buildStripeConfirmCallbackData(String result) {
        try {
            return objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                put("result", result);
                put("simulated", true);
                put("timestamp", System.currentTimeMillis());
            }});
        } catch (Exception e) {
            return "{\"result\":\"" + result + "\",\"simulated\":true}";
        }
    }

    private String buildStripeWebhookCallbackData(StripeWebhookEvent event) {
        try {
            return objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                put("type", event.getType());
                put("status", event.getStatus());
                put("simulated", true);
                put("timestamp", System.currentTimeMillis());
            }});
        } catch (Exception e) {
            return "{\"type\":\"" + event.getType() + "\",\"simulated\":true}";
        }
    }

    private String buildPayPalCaptureCallbackData(String result) {
        try {
            return objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                put("result", result);
                put("simulated", true);
                put("timestamp", System.currentTimeMillis());
            }});
        } catch (Exception e) {
            return "{\"result\":\"" + result + "\",\"simulated\":true}";
        }
    }
}
