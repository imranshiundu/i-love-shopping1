package com.iloveshopping.service;

import com.iloveshopping.config.MpesaProperties;
import com.iloveshopping.dto.payment.MpesaStkPushRequest;
import com.iloveshopping.dto.payment.MpesaStkPushResponse;
import com.iloveshopping.dto.payment.PaymentResponse;
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MpesaService {

    private final MpesaProperties mpesaProperties;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final OrderMessagePublisher orderMessagePublisher;
    private final EmailService emailService;

    private String cachedAccessToken;
    private Instant tokenExpiryTime;

    public String getAccessToken() {
        if (!mpesaProperties.isConfigured()) {
            throw new PaymentException("M-Pesa credentials not configured");
        }
        if (cachedAccessToken != null && tokenExpiryTime != null && Instant.now().isBefore(tokenExpiryTime)) {
            return cachedAccessToken;
        }

        log.debug("Requesting M-Pesa OAuth token from {}", mpesaProperties.getBaseUrl());

        try {
            String authKey = mpesaProperties.getConsumerKey() + ":" + mpesaProperties.getConsumerSecret();
            String encodedAuth = Base64.getEncoder().encodeToString(authKey.getBytes(StandardCharsets.UTF_8));

            String authUrl = mpesaProperties.getBaseUrl() + "/oauth/v1/generate?grant_type=client_credentials";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encodedAuth);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    authUrl, HttpMethod.GET, request, JsonNode.class
            );

            JsonNode node = response.getBody();
            if (node == null || !node.has("access_token")) {
                throw new PaymentException("Failed to get M-Pesa access token - invalid response");
            }

            cachedAccessToken = node.get("access_token").asText();
            int expiresIn = node.get("expires_in").asInt();
            tokenExpiryTime = Instant.now().plusSeconds(expiresIn - 30);

            log.info("M-Pesa OAuth token obtained, expires in {}s", expiresIn);
            return cachedAccessToken;
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("M-Pesa OAuth failed: {}", e.getMessage(), e);
            throw new PaymentException("Failed to authenticate with M-Pesa API: " + e.getMessage());
        }
    }

    @Transactional
    public MpesaStkPushResponse initiateStkPush(MpesaStkPushRequest request) {
        if (!mpesaProperties.isConfigured()) {
            throw new PaymentException("M-Pesa is not configured. Set MPESA_CONSUMER_KEY, MPESA_CONSUMER_SECRET, MPESA_SHORTCODE, and MPESA_PASSKEY environment variables.");
        }

        log.info("Initiating M-Pesa STK Push — order: {} amount: {} phone: {}",
                request.getOrderId(), request.getAmount(), request.getPhoneNumber());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));

        if (request.getAmount().compareTo(order.getTotal()) != 0) {
            log.warn("Amount mismatch: request={} order={}", request.getAmount(), order.getTotal());
            throw new PaymentException("Payment amount does not match order total");
        }

        String phoneNumber = normalizePhone(request.getPhoneNumber());
        String accountRef = truncateAccountRef(
                request.getAccountReference() != null ? request.getAccountReference() : order.getNumber()
        );

        try {
            String accessToken = getAccessToken();
            String timestamp = getTimestamp();
            String password = generatePassword(timestamp);

            ObjectNode stkRequest = objectMapper.createObjectNode();
            stkRequest.put("BusinessShortCode", mpesaProperties.getShortcode());
            stkRequest.put("Password", password);
            stkRequest.put("Timestamp", timestamp);
            stkRequest.put("TransactionType", "CustomerPayBillOnline");
            stkRequest.put("Amount", String.valueOf(request.getAmount().intValue()));
            stkRequest.put("PartyA", phoneNumber);
            stkRequest.put("PartyB", mpesaProperties.getShortcode());
            stkRequest.put("PhoneNumber", phoneNumber);
            stkRequest.put("IdentifierType", "17");
            stkRequest.put("Remarks", "Payment for order " + order.getNumber());
            stkRequest.put("CallBackURL", mpesaProperties.getCallbackUrl());
            stkRequest.put("AccountReference", accountRef);
            stkRequest.put("TransactionDesc", request.getTransactionDesc() != null
                    ? request.getTransactionDesc() : "Order " + order.getNumber());

            Payment payment = Payment.builder()
                    .order(order)
                    .provider(Payment.PaymentProvider.MPESA)
                    .providerId("PENDING_STK")
                    .amount(request.getAmount())
                    .currency("KES")
                    .status(Payment.PaymentStatus.PENDING)
                    .metadata(buildInitMetadata(phoneNumber, accountRef))
                    .build();
            payment = paymentRepository.save(payment);

            String stkJson = objectMapper.writeValueAsString(stkRequest);
            String stkUrl = mpesaProperties.getBaseUrl() + "/mpesa/stkpush/v1/processrequest";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> httpRequest = new HttpEntity<>(stkJson, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(stkUrl, httpRequest, String.class);

            JsonNode responseNode = objectMapper.readTree(response.getBody());

            String responseCode = responseNode.has("ResponseCode")
                    ? responseNode.get("ResponseCode").asText() : null;
            String merchantRequestId = responseNode.has("MerchantRequestID")
                    ? responseNode.get("MerchantRequestID").asText() : null;
            String checkoutRequestId = responseNode.has("CheckoutRequestID")
                    ? responseNode.get("CheckoutRequestID").asText() : null;
            String customerMessage = responseNode.has("CustomerMessage")
                    ? responseNode.get("CustomerMessage").asText() : null;
            String responseDescription = responseNode.has("ResponseDescription")
                    ? responseNode.get("ResponseDescription").asText() : null;

            if (!"0".equals(responseCode)) {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setMetadata(buildFailedMetadata(responseCode, responseDescription));
                paymentRepository.save(payment);
                throw new PaymentException("STK Push rejected: " + responseDescription);
            }

            payment.setProviderId(checkoutRequestId);
            payment.setMetadata(buildStkMetadata(checkoutRequestId, merchantRequestId, phoneNumber));
            paymentRepository.save(payment);

            log.info("STK Push sent — checkoutRequestId={}, merchantRequestId={}", checkoutRequestId, merchantRequestId);

            return MpesaStkPushResponse.builder()
                    .merchantRequestId(merchantRequestId)
                    .checkoutRequestId(checkoutRequestId)
                    .responseCode(responseCode)
                    .responseDescription(responseDescription)
                    .customerMessage(customerMessage)
                    .build();

        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("STK Push failed: {}", e.getMessage(), e);
            throw new PaymentException("Failed to initiate M-Pesa payment: " + e.getMessage());
        }
    }

    @Transactional
    public void processMpesaCallback(String callbackBody) {
        log.info("M-Pesa callback received");

        try {
            JsonNode callbackJson = objectMapper.readTree(callbackBody);
            JsonNode stkCallback = callbackJson.get("Body").get("stkCallback");

            String checkoutRequestId = stkCallback.get("CheckoutRequestID").asText();
            int resultCode = stkCallback.get("ResultCode").asInt();
            String resultDesc = stkCallback.get("ResultDesc").asText();

            Optional<Payment> paymentOpt = paymentRepository.findByProviderId(checkoutRequestId);
            if (paymentOpt.isEmpty()) {
                log.warn("No payment found for checkoutRequestId={}", checkoutRequestId);
                return;
            }

            Payment payment = paymentOpt.get();
            if (payment.getStatus() == Payment.PaymentStatus.SUCCEEDED) {
                log.info("Payment already succeeded, ignoring duplicate callback for {}", checkoutRequestId);
                return;
            }

            payment.setCallbackData(callbackBody);

            if (resultCode == 0) {
                JsonNode items = stkCallback.get("CallbackMetadata").get("Item");
                BigDecimal amount = null;
                String mpesaReceipt = null;
                String phoneNumber = null;

                for (JsonNode item : items) {
                    String name = item.get("Name").asText();
                    switch (name) {
                        case "Amount" -> amount = new BigDecimal(item.get("Value").asText());
                        case "MpesaReceiptNumber" -> mpesaReceipt = item.get("Value").asText();
                        case "PhoneNumber" -> phoneNumber = item.get("Value").asText();
                    }
                }

                payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
                if (amount != null) payment.setAmount(amount);
                payment.setProviderId(checkoutRequestId);
                paymentRepository.save(payment);

                onPaymentSucceeded(payment, mpesaReceipt);

                log.info("M-Pesa payment SUCCESS — checkout={}, receipt={}", checkoutRequestId, mpesaReceipt);

            } else {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                paymentRepository.save(payment);

                String status = describeResultCode(resultCode);
                log.warn("M-Pesa payment FAILED — checkout={}, code={}, desc={} ({})",
                        checkoutRequestId, resultCode, resultDesc, status);
            }

        } catch (Exception e) {
            log.error("Failed to process M-Pesa callback: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void processMpesaTimeout(String timeoutBody) {
        log.warn("M-Pesa timeout received: {}", timeoutBody);

        try {
            JsonNode timeoutJson = objectMapper.readTree(timeoutBody);
            String checkoutRequestId = timeoutJson.has("CheckoutRequestID")
                    ? timeoutJson.get("CheckoutRequestID").asText() : null;

            if (checkoutRequestId != null) {
                paymentRepository.findByProviderId(checkoutRequestId).ifPresent(payment -> {
                    if (payment.getStatus() != Payment.PaymentStatus.SUCCEEDED) {
                        payment.setStatus(Payment.PaymentStatus.FAILED);
                        payment.setCallbackData(timeoutBody);
                        paymentRepository.save(payment);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Failed to process M-Pesa timeout: {}", e.getMessage(), e);
        }
    }

    public MpesaStkPushResponse queryStkPushStatus(String checkoutRequestId) {
        log.info("Querying STK Push status for {}", checkoutRequestId);

        try {
            String accessToken = getAccessToken();
            String timestamp = getTimestamp();
            String password = generatePassword(timestamp);

            ObjectNode queryRequest = objectMapper.createObjectNode();
            queryRequest.put("BusinessShortCode", mpesaProperties.getShortcode());
            queryRequest.put("Password", password);
            queryRequest.put("Timestamp", timestamp);
            queryRequest.put("CheckoutRequestID", checkoutRequestId);

            String statusUrl = mpesaProperties.getBaseUrl() + "/mpesa/stkpushquery/v1/query";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> httpRequest = new HttpEntity<>(objectMapper.writeValueAsString(queryRequest), headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    statusUrl, HttpMethod.POST, httpRequest, JsonNode.class
            );

            JsonNode node = response.getBody();
            if (node == null) {
                throw new PaymentException("Empty response from STK query");
            }

            String responseCode = node.has("ResponseCode") ? node.get("ResponseCode").asText() : null;
            String responseDescription = node.has("ResponseDescription") ? node.get("ResponseDescription").asText() : null;
            String resultCode = node.has("ResultCode") ? node.get("ResultCode").asText() : null;
            String resultDesc = node.has("ResultDesc") ? node.get("ResultDesc").asText() : null;

            log.info("STK query result — checkout={}, resultCode={}, desc={}", checkoutRequestId, resultCode, resultDesc);

            return MpesaStkPushResponse.builder()
                    .merchantRequestId(null)
                    .checkoutRequestId(checkoutRequestId)
                    .responseCode(responseCode)
                    .responseDescription(responseDescription)
                    .customerMessage(resultDesc)
                    .build();

        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("STK query failed: {}", e.getMessage(), e);
            throw new PaymentException("Failed to query M-Pesa payment status: " + e.getMessage());
        }
    }

    public PaymentResponse getPaymentByCheckoutRequestId(String checkoutRequestId) {
        Payment payment = paymentRepository.findByProviderId(checkoutRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "checkoutRequestId", checkoutRequestId));
        return PaymentResponse.from(payment);
    }

    public List<PaymentResponse> getOrderPayments(String orderNumber) {
        Order order = orderRepository.findByNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "number", orderNumber));
        return paymentRepository.findByOrderId(order.getId()).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    public MpesaStkPushResponse retryPayment(String orderNumber) {
        Order order = orderRepository.findByNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "number", orderNumber));

        MpesaStkPushRequest request = MpesaStkPushRequest.builder()
                .orderId(order.getId())
                .amount(order.getTotal())
                .phoneNumber(null)
                .accountReference(order.getNumber())
                .transactionDesc("Order payment retry")
                .build();

        return initiateStkPush(request);
    }

    @Transactional
    public void onPaymentSucceeded(Payment payment, String mpesaReceipt) {
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

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("254")) return digits;
        if (digits.startsWith("0")) return "254" + digits.substring(1);
        if (digits.startsWith("7") || digits.startsWith("1")) return "254" + digits;
        return digits;
    }

    private String truncateAccountRef(String ref) {
        if (ref == null) return "ORDER";
        String clean = ref.replaceAll("[^a-zA-Z0-9]", "");
        return clean.length() > 12 ? clean.substring(0, 12) : clean;
    }

    private String getTimestamp() {
        ZonedDateTime kenyaTime = ZonedDateTime.now(ZoneId.of("Africa/Nairobi"));
        return kenyaTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String generatePassword(String timestamp) {
        String data = mpesaProperties.getShortcode() + mpesaProperties.getPasskey() + timestamp;
        return Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
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

    private String buildInitMetadata(String phone, String accountRef) {
        try {
            return objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
                put("customerPhone", phone);
                put("accountReference", accountRef);
                put("initiatedAt", Instant.now().toString());
            }});
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildStkMetadata(String checkoutRequestId, String merchantRequestId, String phone) {
        try {
            return objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
                put("checkoutRequestId", checkoutRequestId);
                put("merchantRequestId", merchantRequestId);
                put("customerPhone", phone);
                put("initiatedAt", Instant.now().toString());
            }});
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildFailedMetadata(String responseCode, String description) {
        try {
            return objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
                put("responseCode", responseCode);
                put("responseDescription", description);
                put("failedAt", Instant.now().toString());
            }});
        } catch (Exception e) {
            return "{}";
        }
    }
}
