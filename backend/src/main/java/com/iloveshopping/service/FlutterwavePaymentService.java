package com.iloveshopping.service;

import com.iloveshopping.config.FlutterwaveProperties;
import com.iloveshopping.entity.Order;
import com.iloveshopping.entity.Payment;
import com.iloveshopping.exception.PaymentException;
import com.iloveshopping.exception.ResourceNotFoundException;
import com.iloveshopping.messaging.OrderMessagePublisher;
import com.iloveshopping.repository.OrderRepository;
import com.iloveshopping.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FlutterwavePaymentService {

    private final FlutterwaveProperties flutterwaveProperties;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final OrderMessagePublisher orderMessagePublisher;
    private final EmailService emailService;

    private static final String FLW_BASE_URL = "https://api.flutterwave.com/v3";

    public boolean isConfigured() {
        return flutterwaveProperties.getSecretKey() != null && !flutterwaveProperties.getSecretKey().isBlank();
    }

    @Transactional
    public Map<String, Object> initializeTransaction(String orderId, BigDecimal amount, String currency, String customerEmail, String customerName) {
        if (!isConfigured()) {
            throw new PaymentException("Flutterwave is not configured. Add FLUTTERWAVE_SECRET_KEY to your environment.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        String txRef = "ILS-" + order.getNumber() + "-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("Initializing Flutterwave transaction for order {} amount {} {} ref={}", order.getNumber(), amount, currency, txRef);

        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("tx_ref", txRef);
            payload.put("amount", amount.toString());
            payload.put("currency", currency != null ? currency.toUpperCase() : "KES");
            payload.put("redirect_url", flutterwaveProperties.getRedirectUrl() + "?order=" + order.getNumber());

            ObjectNode customer = payload.putObject("customer");
            customer.put("email", customerEmail != null ? customerEmail : "test@example.com");
            if (customerName != null) customer.put("name", customerName);

            ObjectNode customization = payload.putObject("customization");
            customization.put("title", "i-Love-Shopping");
            customization.put("description", "Payment for order " + order.getNumber());

            Payment payment = Payment.builder()
                    .order(order)
                    .provider(Payment.PaymentProvider.FLUTTERWAVE)
                    .providerId(txRef)
                    .amount(amount)
                    .currency(currency != null ? currency.toUpperCase() : "KES")
                    .status(Payment.PaymentStatus.PENDING)
                    .metadata(buildMetadata(txRef, "initialized"))
                    .build();
            paymentRepository.save(payment);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + flutterwaveProperties.getSecretKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(FLW_BASE_URL + "/payments", request, String.class);

            JsonNode responseBody = objectMapper.readTree(response.getBody());
            String status = responseBody.get("status").asText();

            if (!"success".equals(status)) {
                String message = responseBody.has("message") ? responseBody.get("message").asText() : "Unknown error";
                throw new PaymentException("Flutterwave initialization failed: " + message);
            }

            JsonNode data = responseBody.get("data");
            String checkoutUrl = data.get("link").asText();
            String flwTransactionId = data.has("id") ? data.get("id").asText() : null;

            payment.setProviderId(txRef);
            payment.setMetadata(buildMetadata(txRef, flwTransactionId, "initialized"));
            paymentRepository.save(payment);

            Map<String, Object> result = new HashMap<>();
            result.put("transactionRef", txRef);
            result.put("checkoutUrl", checkoutUrl);
            result.put("flwTransactionId", flwTransactionId);
            result.put("orderId", order.getId());
            result.put("paymentId", payment.getId());

            log.info("Flutterwave transaction initialized: ref={} url={}", txRef, checkoutUrl);
            return result;

        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Flutterwave initialization failed: {}", e.getMessage(), e);
            throw new PaymentException("Failed to initialize Flutterwave payment: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> verifyTransaction(String transactionRef) {
        if (!isConfigured()) {
            throw new PaymentException("Flutterwave is not configured");
        }

        Payment payment = paymentRepository.findByProviderId(transactionRef)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionRef", transactionRef));

        log.info("Verifying Flutterwave transaction: {}", transactionRef);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + flutterwaveProperties.getSecretKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    FLW_BASE_URL + "/transactions/verify?tx_ref=" + transactionRef,
                    HttpMethod.GET, request, String.class);

            JsonNode responseBody = objectMapper.readTree(response.getBody());
            String status = responseBody.get("status").asText();

            if (!"success".equals(status)) {
                String message = responseBody.has("message") ? responseBody.get("message").asText() : "Verification failed";
                throw new PaymentException("Flutterwave verification failed: " + message);
            }

            JsonNode data = responseBody.get("data");
            String flwStatus = data.get("status").asText();
            String flwTransactionId = data.has("id") ? data.get("id").asText() : null;

            Payment.PaymentStatus newStatus = "successful".equals(flwStatus)
                    ? Payment.PaymentStatus.SUCCEEDED
                    : "failed".equals(flwStatus) || "cancelled".equals(flwStatus)
                    ? Payment.PaymentStatus.FAILED
                    : Payment.PaymentStatus.PROCESSING;

            payment.setStatus(newStatus);
            payment.setMetadata(buildMetadata(transactionRef, flwTransactionId, flwStatus));
            paymentRepository.save(payment);

            if (newStatus == Payment.PaymentStatus.SUCCEEDED) {
                onPaymentSucceeded(payment);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("transactionRef", transactionRef);
            result.put("flwTransactionId", flwTransactionId);
            result.put("status", flwStatus);
            result.put("orderId", payment.getOrder().getId());
            result.put("paymentId", payment.getId());

            log.info("Flutterwave verification: ref={} status={}", transactionRef, flwStatus);
            return result;

        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Flutterwave verification failed: {}", e.getMessage(), e);
            throw new PaymentException("Failed to verify Flutterwave payment: " + e.getMessage());
        }
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
            if (order.getUser() != null && order.getUser().getEmail() != null) {
                try {
                    emailService.sendOrderConfirmation(
                            order.getUser().getEmail(), order.getNumber(), order.getId().toString());
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

    private String buildMetadata(String txRef, String status) {
        try {
            var data = new LinkedHashMap<String, Object>();
            data.put("txRef", txRef);
            data.put("status", status);
            data.put("timestamp", java.time.Instant.now().toString());
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildMetadata(String txRef, String flwId, String status) {
        try {
            var data = new LinkedHashMap<String, Object>();
            data.put("txRef", txRef);
            data.put("flwTransactionId", flwId);
            data.put("status", status);
            data.put("timestamp", java.time.Instant.now().toString());
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }
}
