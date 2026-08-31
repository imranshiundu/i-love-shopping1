package com.iloveshopping.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iloveshopping.config.MpesaProperties;
import com.iloveshopping.dto.payment.MpesaStkPushRequest;
import com.iloveshopping.dto.payment.MpesaStkPushResponse;
import com.iloveshopping.entity.Order;
import com.iloveshopping.entity.OrderItem;
import com.iloveshopping.entity.Payment;
import com.iloveshopping.entity.Product;
import com.iloveshopping.exception.PaymentException;
import com.iloveshopping.exception.ResourceNotFoundException;
import com.iloveshopping.messaging.OrderMessagePublisher;
import com.iloveshopping.repository.OrderRepository;
import com.iloveshopping.repository.PaymentRepository;
import com.iloveshopping.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MpesaService {

    private final MpesaProperties mpesaProperties;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final OrderMessagePublisher orderMessagePublisher;
    private final EmailService emailService;

    private volatile String cachedToken;
    private volatile Instant tokenExpiry;

    public String getAccessToken() {
        if (!mpesaProperties.isConfigured()) {
            throw new PaymentException("M-Pesa credentials not configured");
        }
        if (cachedToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        try {
            String creds = mpesaProperties.getConsumerKey() + ":" + mpesaProperties.getConsumerSecret();
            String encoded = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));

            String url = mpesaProperties.getBaseUrl() + "/oauth/v1/generate?grant_type=client_credentials";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encoded);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<JsonNode> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
            JsonNode body = resp.getBody();
            if (body == null || !body.has("access_token")) {
                throw new PaymentException("M-Pesa OAuth: no access_token in response");
            }
            cachedToken = body.get("access_token").asText();
            int expiresIn = body.path("expires_in").asInt(3600);
            tokenExpiry = Instant.now().plusSeconds(expiresIn - 30);
            log.info("M-Pesa OAuth token obtained, expires in {}s", expiresIn);
            return cachedToken;
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("M-Pesa OAuth failed: {}", e.getMessage());
            throw new PaymentException("M-Pesa auth failed: " + e.getMessage());
        }
    }

    @Transactional
    public MpesaStkPushResponse initiateStkPush(MpesaStkPushRequest request) {
        if (!mpesaProperties.isConfigured()) {
            throw new PaymentException("M-Pesa not configured");
        }

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));

        // Server is source of truth — ignore client-provided amount entirely
        BigDecimal amount = order.getTotal();

        log.info("STK Push: order={} amount={} phone={}",
                request.getOrderId(), amount, request.getPhoneNumber());

        if (order.getStatus() != Order.OrderStatus.PENDING
                && order.getStatus() != Order.OrderStatus.EXPIRED) {
            throw new PaymentException("Order is not payable at status: " + order.getStatus());
        }

        String phone = normalizePhone(request.getPhoneNumber());
        String accountRef = truncate(request.getAccountReference() != null
                ? request.getAccountReference() : order.getNumber());

        // Create pending payment record first (so callback can find it by checkoutRequestId)
        Payment payment = Payment.builder()
                .order(order)
                .provider(Payment.PaymentProvider.MPESA)
                .providerId("PENDING_" + System.currentTimeMillis())
                .amount(amount)
                .currency("KES")
                .status(Payment.PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        try {
            String accessToken = getAccessToken();
            String timestamp = currentTimestamp();
            String password = generatePassword(timestamp);

            ObjectNode body = objectMapper.createObjectNode();
            body.put("BusinessShortCode", mpesaProperties.getShortcode());
            body.put("Password", password);
            body.put("Timestamp", timestamp);
            body.put("TransactionType", "CustomerPayBillOnline");
            body.put("Amount", String.valueOf(amount.intValue()));
            body.put("PartyA", phone);
            body.put("PartyB", mpesaProperties.getShortcode());
            body.put("PhoneNumber", phone);
            body.put("IdentifierType", "4");  // 4 = shortcode (paybill), 1 = MSISDN
            body.put("CallBackURL", mpesaProperties.getCallbackUrl());
            body.put("AccountReference", accountRef);
            body.put("TransactionDesc", request.getTransactionDesc() != null
                    ? request.getTransactionDesc() : "Order " + order.getNumber());

            log.info("STK Push CallBackURL={}", mpesaProperties.getCallbackUrl());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = mpesaProperties.getBaseUrl() + "/mpesa/stkpush/v1/processrequest";
            ResponseEntity<String> resp = restTemplate.postForEntity(
                    url, new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class);

            JsonNode respNode = objectMapper.readTree(resp.getBody());
            String responseCode = textOrNull(respNode, "ResponseCode");
            String checkoutRequestId = textOrNull(respNode, "CheckoutRequestID");
            String merchantRequestId = textOrNull(respNode, "MerchantRequestID");
            String customerMessage = textOrNull(respNode, "CustomerMessage");
            String responseDesc = textOrNull(respNode, "ResponseDescription");

            if (!"0".equals(responseCode) || checkoutRequestId == null) {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                paymentRepository.save(payment);
                throw new PaymentException("STK Push rejected: " + (responseDesc != null ? responseDesc : "code " + responseCode));
            }

            // Update payment with checkoutRequestId as providerId
            payment.setProviderId(checkoutRequestId);
            paymentRepository.save(payment);

            log.info("STK Push sent: checkout={} merchant={}", checkoutRequestId, merchantRequestId);

            return MpesaStkPushResponse.builder()
                    .merchantRequestId(merchantRequestId)
                    .checkoutRequestId(checkoutRequestId)
                    .responseCode(responseCode)
                    .responseDescription(responseDesc)
                    .customerMessage(customerMessage)
                    .build();

        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("STK Push failed: {}", e.getMessage(), e);
            throw new PaymentException("STK Push failed: " + e.getMessage());
        }
    }

    @Transactional
    public void processMpesaCallback(String callbackBody) {
        log.info("M-Pesa callback received");
        try {
            JsonNode root = objectMapper.readTree(callbackBody);
            // Safaricom wraps in {"Body":{"stkCallback":{...}}} but some proxies flatten it
            JsonNode stk = root.path("Body").path("stkCallback");
            if (stk.isMissingNode() || stk.isNull()) {
                stk = root.path("stkCallback");
            }
            if (stk.isMissingNode() || stk.isNull()) {
                log.warn("M-Pesa callback: missing Body.stkCallback. Raw body: {}", callbackBody);
                return;
            }
            String checkoutRequestId = textOrNull(stk, "CheckoutRequestID");
            if (checkoutRequestId == null) {
                log.warn("M-Pesa callback: missing CheckoutRequestID");
                return;
            }
            int resultCode = stk.path("ResultCode").asInt(-1);
            String resultDesc = textOrNull(stk, "ResultDesc");

            Optional<Payment> paymentOpt = paymentRepository.findByProviderId(checkoutRequestId);
            if (paymentOpt.isEmpty()) {
                log.warn("M-Pesa callback: no payment for checkoutRequestId={}", checkoutRequestId);
                return;
            }
            Payment payment = paymentOpt.get();
            if (payment.getStatus() == Payment.PaymentStatus.SUCCEEDED) {
                log.info("M-Pesa callback: payment {} already succeeded, ignoring", checkoutRequestId);
                return;
            }

            Order order = payment.getOrder();
            if (order.getStatus() != Order.OrderStatus.PENDING
                    && order.getStatus() != Order.OrderStatus.EXPIRED) {
                log.warn("M-Pesa callback: order {} is {} (not payable), ignoring",
                        order.getNumber(), order.getStatus());
                return;
            }

            payment.setCallbackData(callbackBody);

            if (resultCode == 0) {
                BigDecimal amount = null;
                String mpesaReceipt = null;

                JsonNode items = stk.path("CallbackMetadata").path("Item");
                if (items.isArray()) {
                    for (JsonNode item : items) {
                        String name = textOrNull(item, "Name");
                        if (name == null) continue;
                        JsonNode val = item.path("Value");
                        if (val.isMissingNode() || val.isNull()) continue;
                        switch (name) {
                            case "Amount":
                                try { amount = new BigDecimal(val.asText()); }
                                catch (NumberFormatException nfe) {
                                    log.warn("Invalid Amount in callback: {}", val.asText());
                                }
                                break;
                            case "MpesaReceiptNumber":
                                mpesaReceipt = val.asText();
                                break;
                            default: break;
                        }
                    }
                }

                if (amount == null) {
                    log.error("M-Pesa callback: missing Amount in CallbackMetadata for {}", checkoutRequestId);
                    return;
                }
                // M-Pesa STK push only accepts integer amounts, so callback echoes the integer value.
                // Compare against the integer part of the order total, not the exact decimal.
                if (amount.compareTo(BigDecimal.valueOf(order.getTotal().intValue())) != 0) {
                    log.error("M-Pesa callback: amount mismatch order={} got={}", order.getTotal(), amount);
                    markPaymentFailed(payment, order, "amount_mismatch");
                    return;
                }

                // If order was EXPIRED, stock was already restored — re-decrement atomically
                if (order.getStatus() == Order.OrderStatus.EXPIRED) {
                    for (OrderItem item : order.getItems()) {
                        int rows = productRepository.decrementStock(item.getProduct().getId(), item.getQuantity());
                        if (rows == 0) {
                            // stock no longer available
                            for (OrderItem i2 : order.getItems()) {
                                productRepository.incrementStock(i2.getProduct().getId(), i2.getQuantity());
                            }
                            payment.setStatus(Payment.PaymentStatus.FAILED);
                            paymentRepository.save(payment);
                            order.setStatus(Order.OrderStatus.CANCELLED);
                            orderRepository.save(order);
                            notifyFailure(order, "stock_unavailable");
                            return;
                        }
                    }
                }

                payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
                payment.setAmount(amount);
                paymentRepository.save(payment);

                order.setStatus(Order.OrderStatus.CONFIRMED);
                orderRepository.save(order);

                final Order finalOrder = order;
                final String finalReceipt = mpesaReceipt;
                runAfterCommit(() -> {
                    try { orderMessagePublisher.publishOrderPaid(finalOrder); }
                    catch (Exception e) { log.error("publishOrderPaid failed: {}", e.getMessage()); }
                    String email = recipientEmail(finalOrder);
                    if (email != null) {
                        try { emailService.sendOrderConfirmation(finalOrder); }
                        catch (Exception e) { log.error("sendOrderConfirmation failed: {}", e.getMessage()); }
                    }
                });

                log.info("M-Pesa payment SUCCESS checkout={} receipt={}", checkoutRequestId, finalReceipt);

            } else {
                markPaymentFailed(payment, order, describeResultCode(resultCode));
                log.warn("M-Pesa payment FAILED checkout={} code={} desc={}",
                        checkoutRequestId, resultCode, resultDesc);
            }

        } catch (Exception e) {
            log.error("processMpesaCallback failed: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void processMpesaTimeout(String timeoutBody) {
        log.warn("M-Pesa timeout received");
        try {
            JsonNode root = objectMapper.readTree(timeoutBody);
            JsonNode stk = root.path("Body").path("stkCallback");
            String checkoutRequestId = !stk.isMissingNode() ? textOrNull(stk, "CheckoutRequestID") : null;
            if (checkoutRequestId == null) {
                checkoutRequestId = textOrNull(root, "CheckoutRequestID");
            }
            if (checkoutRequestId == null) return;

            Optional<Payment> paymentOpt = paymentRepository.findByProviderId(checkoutRequestId);
            if (paymentOpt.isEmpty()) return;
            Payment payment = paymentOpt.get();
            if (payment.getStatus() == Payment.PaymentStatus.SUCCEEDED) return;

            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setCallbackData(timeoutBody);
            paymentRepository.save(payment);

            Order order = payment.getOrder();
            if (order.getStatus() == Order.OrderStatus.PENDING
                    || order.getStatus() == Order.OrderStatus.EXPIRED) {
                markPaymentFailed(payment, order, "timeout_or_expired");
            }
        } catch (Exception e) {
            log.error("processMpesaTimeout failed: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public MpesaStkPushResponse queryStkStatus(String checkoutRequestId) {
        // First check local payment record — callback may have already processed it
        Optional<Payment> localPayment = paymentRepository.findByProviderId(checkoutRequestId);
        if (localPayment.isPresent()) {
            Payment p = localPayment.get();
            if (p.getStatus() == Payment.PaymentStatus.SUCCEEDED) {
                return MpesaStkPushResponse.builder()
                        .checkoutRequestId(checkoutRequestId)
                        .responseCode("0")
                        .responseDescription("The service request is processed successfully.")
                        .customerMessage("Payment completed successfully.")
                        .build();
            }
            if (p.getStatus() == Payment.PaymentStatus.FAILED) {
                return MpesaStkPushResponse.builder()
                        .checkoutRequestId(checkoutRequestId)
                        .responseCode("1")
                        .responseDescription("The transaction was cancelled or failed.")
                        .customerMessage("Payment failed.")
                        .build();
            }
        }

        try {
            String token = getAccessToken();
            String timestamp = currentTimestamp();
            String password = generatePassword(timestamp);

            ObjectNode body = objectMapper.createObjectNode();
            body.put("BusinessShortCode", mpesaProperties.getShortcode());
            body.put("Password", password);
            body.put("Timestamp", timestamp);
            body.put("CheckoutRequestID", checkoutRequestId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = mpesaProperties.getBaseUrl() + "/mpesa/stkpushquery/v1/query";
            ResponseEntity<JsonNode> resp = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
                    JsonNode.class);

            JsonNode n = resp.getBody();
            if (n == null) throw new PaymentException("Empty STK query response");

            // ResultCode is the authoritative indicator from Daraja.
            //  0   = success
            //  1032 = cancelled by user (terminal)
            //  1037 = timeout / still in-flight — NOT terminal; user may still be paying
            //  2001 = wrong PIN (declined)
            //  1    = insufficient funds / declined
            String resultCode = textOrNull(n, "ResultCode");
            String resultDesc = textOrNull(n, "ResultDesc");
            if (resultCode != null) {
                if (resultCode.equals("0") && localPayment.isPresent()) {
                    markQuerySucceeded(localPayment.get());
                    return MpesaStkPushResponse.builder()
                            .checkoutRequestId(checkoutRequestId)
                            .responseCode("0")
                            .responseDescription("The service request is processed successfully.")
                            .customerMessage("Payment completed successfully.")
                            .build();
                }
                // 1037 = in-flight/timeout — do NOT mark failed or cancel the order.
                // The customer may still be entering their PIN. Return an in-progress
                // indicator so the frontend keeps polling.
                if (resultCode.equals("1037")) {
                    return MpesaStkPushResponse.builder()
                            .checkoutRequestId(checkoutRequestId)
                            .responseCode("1037")
                            .responseDescription("The push request is still being processed or has not been completed.")
                            .customerMessage("Waiting for payment confirmation...")
                            .build();
                }
                // Genuinely terminal failures
                if (localPayment.isPresent() && isTerminalFailureCode(parseIntSafe(resultCode))) {
                    markQueryFailed(localPayment.get(), describeResultCode(parseIntSafe(resultCode)));
                    return MpesaStkPushResponse.builder()
                            .checkoutRequestId(checkoutRequestId)
                            .responseCode("1")
                            .responseDescription(resultDesc != null ? resultDesc : "The transaction failed.")
                            .customerMessage(resultDesc != null ? resultDesc : "Payment failed.")
                            .build();
                }
            }

            // Still in flight — return the raw query response for the frontend to keep polling
            return MpesaStkPushResponse.builder()
                    .checkoutRequestId(checkoutRequestId)
                    .responseCode(textOrNull(n, "ResponseCode"))
                    .responseDescription(textOrNull(n, "ResponseDescription"))
                    .customerMessage(resultDesc != null ? resultDesc : textOrNull(n, "ResponseDescription"))
                    .build();
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("STK query failed: {}", e.getMessage());
            throw new PaymentException("STK query failed: " + e.getMessage());
        }
    }

    @Transactional
    protected void markQuerySucceeded(Payment payment) {
        if (payment.getStatus() == Payment.PaymentStatus.SUCCEEDED) return;
        payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        if (order.getStatus() != Order.OrderStatus.CONFIRMED) {
            order.setStatus(Order.OrderStatus.CONFIRMED);
            orderRepository.save(order);
        }
        final Order finalOrder = order;
        runAfterCommit(() -> {
            try { orderMessagePublisher.publishOrderPaid(finalOrder); }
            catch (Exception e) { log.error("publishOrderPaid failed: {}", e.getMessage()); }
            String email = recipientEmail(finalOrder);
            if (email != null) {
                try { emailService.sendOrderConfirmation(finalOrder); }
                catch (Exception e) { log.error("sendOrderConfirmation failed: {}", e.getMessage()); }
            }
        });
        log.info("M-Pesa STK query: payment SUCCESS checkout={}", payment.getProviderId());
    }

    @Transactional
    protected void markQueryFailed(Payment payment, String reason) {
        markPaymentFailed(payment, payment.getOrder(), reason);
        log.warn("M-Pesa STK query: payment FAILED checkout={} reason={}", payment.getProviderId(), reason);
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }

    private boolean isTerminalFailureCode(int code) {
        return switch (code) {
            case 1032, 2001, 1, 2007, 2026, 2002, 2005, 2006 -> true;
            default -> false;
        };
    }

    @Transactional
    public MpesaStkPushResponse retryPayment(String orderNumber, String phoneNumber) {
        Order order = orderRepository.findByNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));

        // Allow retry on PENDING, EXPIRED, or CANCELLED (re-open a cancelled order)
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            // Re-check stock and re-decrement before re-opening
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                if (product.getStock() < item.getQuantity()) {
                    throw new PaymentException("Some items are no longer available. Please place a new order.");
                }
            }
            for (OrderItem item : order.getItems()) {
                productRepository.decrementStock(item.getProduct().getId(), item.getQuantity());
            }
            order.setStatus(Order.OrderStatus.PENDING);
            orderRepository.save(order);
        } else if (order.getStatus() != Order.OrderStatus.PENDING
                && order.getStatus() != Order.OrderStatus.EXPIRED) {
            throw new PaymentException("Order is not payable at status: " + order.getStatus());
        }

        MpesaStkPushRequest req = MpesaStkPushRequest.builder()
                .orderId(order.getId())
                .amount(order.getTotal())
                .phoneNumber(phoneNumber)
                .accountReference(order.getNumber())
                .transactionDesc("Retry payment " + order.getNumber())
                .build();

        return initiateStkPush(req);
    }

    // ---- helpers ----

    private void markPaymentFailed(Payment payment, Order order, String reason) {
        // Guard against double-processing (idempotency)
        if (payment.getStatus() == Payment.PaymentStatus.FAILED
                || payment.getStatus() == Payment.PaymentStatus.SUCCEEDED) {
            log.info("markPaymentFailed: payment {} already in terminal state {}, skipping", payment.getProviderId(), payment.getStatus());
            return;
        }
        if (order.getStatus() == Order.OrderStatus.CANCELLED
                || order.getStatus() == Order.OrderStatus.CONFIRMED
                || order.getStatus() == Order.OrderStatus.DELIVERED) {
            log.info("markPaymentFailed: order {} already in terminal state {}, skipping", order.getNumber(), order.getStatus());
            return;
        }
        Payment.PaymentStatus payStatus = reason != null && reason.toLowerCase().contains("cancel")
                ? Payment.PaymentStatus.CANCELLED
                : Payment.PaymentStatus.FAILED;
        payment.setStatus(payStatus);
        paymentRepository.save(payment);
        for (OrderItem item : order.getItems()) {
            productRepository.incrementStock(item.getProduct().getId(), item.getQuantity());
        }
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
        notifyFailure(order, reason);
    }

    private void notifyFailure(Order order, String reason) {
        final Order finalOrder = order;
        runAfterCommit(() -> {
            try { orderMessagePublisher.publishOrderCancelled(finalOrder); }
            catch (Exception e) { log.error("publishOrderCancelled failed: {}", e.getMessage()); }
        });
    }

    private String recipientEmail(Order order) {
        if (order.getUser() != null && order.getUser().getEmail() != null) {
            return order.getUser().getEmail();
        }
        if (order.getGuestEmail() != null && !order.getGuestEmail().isBlank()) {
            return order.getGuestEmail();
        }
        return null;
    }

    private void runAfterCommit(Runnable r) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() { r.run(); }
                    });
        } else {
            r.run();
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("254")) return digits;
        if (digits.startsWith("0")) return "254" + digits.substring(1);
        if (digits.startsWith("7") || digits.startsWith("1")) return "254" + digits;
        return digits;
    }

    private String truncate(String s) {
        if (s == null) return "ORDER";
        String c = s.replaceAll("[^a-zA-Z0-9]", "");
        return c.length() > 12 ? c.substring(0, 12) : c;
    }

    private String currentTimestamp() {
        return ZonedDateTime.now(ZoneId.of("Africa/Nairobi"))
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String generatePassword(String timestamp) {
        String raw = mpesaProperties.getShortcode() + mpesaProperties.getPasskey() + timestamp;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String textOrNull(JsonNode n, String field) {
        if (n == null) return null;
        JsonNode v = n.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private String describeResultCode(int code) {
        return switch (code) {
            case 0 -> "success";
            case 1032 -> "cancelled_by_user";
            case 1037 -> "timeout_or_expired";
            case 2001 -> "wrong_pin";
            case 1 -> "insufficient_funds";
            case 2007 -> "referred";
            case 2026 -> "debit_account_limit_exceeded";
            default -> "failed_" + code;
        };
    }
}
