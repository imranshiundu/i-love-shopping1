package com.iloveshopping.service;

import com.iloveshopping.config.StripeProperties;
import com.iloveshopping.entity.Order;
import com.iloveshopping.entity.OrderItem;
import com.iloveshopping.entity.Payment;
import com.iloveshopping.exception.PaymentException;
import com.iloveshopping.exception.ResourceNotFoundException;
import com.iloveshopping.messaging.OrderMessagePublisher;
import com.iloveshopping.repository.OrderRepository;
import com.iloveshopping.repository.PaymentRepository;
import com.iloveshopping.repository.ProductRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StripePaymentService {

    private final StripeProperties stripeProperties;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final OrderMessagePublisher orderMessagePublisher;
    private final EmailService emailService;

    @PostConstruct
    public void init() {
        if (stripeProperties.getSecretKey() != null && !stripeProperties.getSecretKey().isBlank()) {
            Stripe.apiKey = stripeProperties.getSecretKey();
            log.info("Stripe API key configured");
        } else {
            log.warn("Stripe secret key not configured — card payments will be unavailable");
        }
    }

    public boolean isConfigured() {
        return stripeProperties.getSecretKey() != null && !stripeProperties.getSecretKey().isBlank();
    }

    @Transactional
    public Map<String, Object> createPaymentIntent(String orderId, BigDecimal clientAmount, String currency) {
        if (!isConfigured()) {
            throw new PaymentException("Stripe is not configured. Add STRIPE_SECRET_KEY to your environment.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Server is source of truth — validate client amount matches order total
        BigDecimal amount = order.getTotal();
        if (clientAmount != null && clientAmount.compareTo(amount) != 0) {
            log.warn("Stripe amount mismatch for order {}: client={} server={}", order.getNumber(), clientAmount, amount);
            throw new PaymentException("Payment amount does not match order total");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING
                && order.getStatus() != Order.OrderStatus.EXPIRED) {
            throw new PaymentException("Order is not payable at status: " + order.getStatus());
        }

        log.info("Creating Stripe PaymentIntent for order {} amount {} {}", order.getNumber(), amount, currency);

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount.movePointRight(2).longValue())
                    .setCurrency(currency != null ? currency.toLowerCase() : "kes")
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    )
                    .putMetadata("orderId", order.getId())
                    .putMetadata("orderNumber", order.getNumber())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            Payment payment = Payment.builder()
                    .order(order)
                    .provider(Payment.PaymentProvider.STRIPE)
                    .providerId(paymentIntent.getId())
                    .amount(amount)
                    .currency(currency != null ? currency.toUpperCase() : "KES")
                    .status(Payment.PaymentStatus.PENDING)
                    .metadata(buildMetadata(paymentIntent.getId(), paymentIntent.getStatus()))
                    .build();
            paymentRepository.save(payment);

            Map<String, Object> response = new HashMap<>();
            response.put("paymentIntentId", paymentIntent.getId());
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("status", paymentIntent.getStatus());
            response.put("orderId", order.getId());
            response.put("paymentId", payment.getId());
            response.put("publishableKey", stripeProperties.getPublishableKey());

            log.info("Stripe PaymentIntent created: {} for order {}", paymentIntent.getId(), order.getNumber());
            return response;

        } catch (StripeException e) {
            log.error("Stripe PaymentIntent creation failed: {}", e.getMessage(), e);
            throw new PaymentException("Failed to create Stripe payment: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> confirmPayment(String paymentIntentId) {
        Payment payment = paymentRepository.findByProviderId(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentIntentId", paymentIntentId));

        if (!isConfigured()) {
            throw new PaymentException("Stripe is not configured");
        }

        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            intent = intent.confirm();

            payment.setStatus(mapStripeStatus(intent.getStatus()));
            payment.setMetadata(buildMetadata(paymentIntentId, intent.getStatus()));
            paymentRepository.save(payment);

            if (payment.getStatus() == Payment.PaymentStatus.SUCCEEDED) {
                onPaymentSucceeded(payment);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("paymentIntentId", paymentIntentId);
            response.put("status", intent.getStatus());
            response.put("orderId", payment.getOrder().getId());
            response.put("paymentId", payment.getId());

            return response;

        } catch (StripeException e) {
            log.error("Stripe payment confirmation failed: {}", e.getMessage(), e);
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setMetadata(buildMetadata(paymentIntentId, "failed: " + e.getMessage()));
            paymentRepository.save(payment);

            Order order = payment.getOrder();
            if (order.getStatus() == Order.OrderStatus.PENDING
                    || order.getStatus() == Order.OrderStatus.EXPIRED) {
                for (OrderItem item : order.getItems()) {
                    productRepository.incrementStock(item.getProduct().getId(), item.getQuantity());
                }
                order.setStatus(Order.OrderStatus.CANCELLED);
                orderRepository.save(order);
                notifyPaymentFailure(order, "card_declined");
            }

            throw new PaymentException("Failed to confirm Stripe payment: " + e.getMessage());
        }
    }

    @Transactional
    public void processWebhook(String payload, String sigHeader) throws com.stripe.exception.SignatureVerificationException {
        if (!isConfigured()) return;

        com.stripe.model.Event event = com.stripe.net.Webhook.constructEvent(
                payload, sigHeader, stripeProperties.getWebhookSecret());

        try {
            String type = event.getType();
            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject().orElse(null);

            if (intent == null) {
                log.warn("Could not deserialize Stripe webhook event data");
                return;
            }

            Payment payment = paymentRepository.findByProviderId(intent.getId()).orElse(null);
            if (payment == null) {
                log.warn("No payment found for Stripe PaymentIntent: {}", intent.getId());
                return;
            }

            switch (type) {
                case "payment_intent.succeeded" -> {
                    payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
                    payment.setMetadata(buildMetadata(intent.getId(), "succeeded"));
                    paymentRepository.save(payment);
                    onPaymentSucceeded(payment);
                    log.info("Stripe webhook: payment succeeded for {}", intent.getId());
                }
                case "payment_intent.payment_failed" -> {
                    payment.setStatus(Payment.PaymentStatus.FAILED);
                    payment.setMetadata(buildMetadata(intent.getId(), "failed"));
                    paymentRepository.save(payment);
                    log.warn("Stripe webhook: payment failed for {}", intent.getId());
                }
                case "payment_intent.processing" -> {
                    payment.setStatus(Payment.PaymentStatus.PROCESSING);
                    payment.setMetadata(buildMetadata(intent.getId(), "processing"));
                    paymentRepository.save(payment);
                }
                default -> log.debug("Unhandled Stripe webhook type: {}", type);
            }
        } catch (Exception e) {
            log.error("Stripe webhook processing failed: {}", e.getMessage(), e);
        }
    }

    private Payment.PaymentStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "succeeded" -> Payment.PaymentStatus.SUCCEEDED;
            case "processing" -> Payment.PaymentStatus.PROCESSING;
            case "requires_payment_method", "requires_confirmation", "requires_action" -> Payment.PaymentStatus.PENDING;
            case "canceled" -> Payment.PaymentStatus.CANCELLED;
            default -> Payment.PaymentStatus.FAILED;
        };
    }

    private void onPaymentSucceeded(Payment payment) {
        Order order = payment.getOrder();
        order.setStatus(Order.OrderStatus.CONFIRMED);
        orderRepository.save(order);

        Runnable notify = () -> {
            try {
                orderMessagePublisher.publishOrderPaid(order);
            } catch (Exception e) {
                log.error("Failed to publish ORDER_PAID event for {}: {}", order.getNumber(), e.getMessage());
            }
            String email = null;
            if (order.getUser() != null && order.getUser().getEmail() != null) email = order.getUser().getEmail();
            else if (order.getGuestEmail() != null && !order.getGuestEmail().isBlank()) email = order.getGuestEmail();
            if (email != null) {
                String finalEmail = email;
                try {
                    emailService.sendOrderConfirmation(
                            finalEmail, order.getNumber(), order.getId().toString());
                } catch (Exception e) {
                    log.error("Failed to send confirmation email for {}: {}", order.getNumber(), e.getMessage());
                }
            }
        };

        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() { notify.run(); }
                    });
        } else {
            notify.run();
        }
    }

    private String buildMetadata(String paymentIntentId, String status) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var data = new java.util.LinkedHashMap<String, Object>();
            data.put("paymentIntentId", paymentIntentId);
            data.put("stripeStatus", status);
            data.put("timestamp", java.time.Instant.now().toString());
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void notifyPaymentFailure(Order order, String reason) {
        try {
            orderMessagePublisher.publishOrderCancelled(order);
        } catch (Exception e) {
            log.error("Failed to publish ORDER_CANCELLED for {}: {}", order.getNumber(), e.getMessage());
        }
        String email = null;
        if (order.getUser() != null && order.getUser().getEmail() != null) email = order.getUser().getEmail();
        else if (order.getGuestEmail() != null && !order.getGuestEmail().isBlank()) email = order.getGuestEmail();
        if (email != null) {
            try {
                emailService.sendPaymentFailed(email, order.getNumber(), order.getId().toString(), reason);
            } catch (Exception e) {
                log.error("Failed to send payment-failed email for {}: {}", order.getNumber(), e.getMessage());
            }
        }
    }
}
