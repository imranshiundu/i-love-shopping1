package com.iloveshopping.service;

import com.iloveshopping.config.MpesaProperties;
import com.iloveshopping.dto.payment.MpesaStkPushRequest;
import com.iloveshopping.dto.payment.MpesaStkPushResponse;
import com.iloveshopping.dto.payment.PaymentResponse;
import com.iloveshopping.entity.Order;
import com.iloveshopping.entity.Payment;
import com.iloveshopping.exception.PaymentException;
import com.iloveshopping.exception.ResourceNotFoundException;
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
import java.util.UUID;
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

    private String cachedAccessToken;
    private Instant tokenExpiryTime;

    private String getAccessToken() {
        if (cachedAccessToken != null && tokenExpiryTime != null && Instant.now().isBefore(tokenExpiryTime)) {
            return cachedAccessToken;
        }

        log.debug("Requesting new M-Pesa access token");

        try {
            String authKey = mpesaProperties.getConsumerKey() + ":" + mpesaProperties.getConsumerSecret();
            String encodedAuth = Base64.getEncoder().encodeToString(authKey.getBytes(StandardCharsets.UTF_8));

            String authUrl = "https://api.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encodedAuth);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    authUrl, HttpMethod.GET, request, JsonNode.class
            );

            JsonNode node = response.getBody();
            if (node == null) {
                throw new PaymentException("Failed to get M-Pesa access token - null response");
            }

            cachedAccessToken = node.get("access_token").asText();
            int expiresIn = node.get("expires_in").asInt();
            tokenExpiryTime = Instant.now().plusSeconds(expiresIn - 30);

            log.info("M-Pesa access token obtained, expires in {} seconds", expiresIn);
            return cachedAccessToken;
        } catch (Exception e) {
            log.error("Failed to get M-Pesa access token: {}", e.getMessage(), e);
            throw new PaymentException("Failed to authenticate with M-Pesa API: " + e.getMessage());
        }
    }

    @Transactional
    public MpesaStkPushResponse initiateStkPush(MpesaStkPushRequest request) {
        log.info("Initiating M-Pesa STK Push for order: {} amount: {} phone: {}",
                request.getOrderId(), request.getAmount(), request.getPhoneNumber());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));

        if (request.getAmount().compareTo(order.getTotal()) != 0) {
            throw new PaymentException("Payment amount does not match order total");
        }

        if (mpesaProperties.isSimulationEnabled()) {
            return simulateStkPush(order, request);
        }

        try {
            String accessToken = getAccessToken();
            String timestamp = getTimestamp();
            String password = generatePassword(timestamp);
            String partyA = request.getPhoneNumber();
            String callbackUrl = mpesaProperties.getCallbackUrl();

            ObjectNode stkRequest = objectMapper.createObjectNode();
            stkRequest.put("BusinessShortCode", mpesaProperties.getShortcode());
            stkRequest.put("Password", password);
            stkRequest.put("Timestamp", timestamp);
            stkRequest.put("TransactionType", "CustomerPayBillOnline");
            stkRequest.put("Amount", String.valueOf(request.getAmount().intValue()));
            stkRequest.put("PartyA", partyA);
            stkRequest.put("PartyB", mpesaProperties.getShortcode());
            stkRequest.put("PhoneNumber", partyA);
            stkRequest.put("IdentifierType", "4");
            stkRequest.put("Remarks", "Payment for order " + order.getNumber());
            stkRequest.put("CallBackURL", callbackUrl);
            stkRequest.put("AccountName", "i-love-shopping");
            stkRequest.put("AccountReference", request.getAccountReference() != null ? request.getAccountReference() : order.getNumber());
            stkRequest.put("TransactionDesc", request.getTransactionDesc() != null ? request.getTransactionDesc() : "Order payment");

            Payment payment = Payment.builder()
                    .order(order)
                    .provider(Payment.PaymentProvider.MPESA)
                    .providerId("PENDING")
                    .amount(request.getAmount())
                    .currency("KES")
                    .status(Payment.PaymentStatus.PENDING)
                    .metadata("{\"checkoutRequestId\": null}")
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

            String checkoutRequestId = responseNode.has("CheckoutRequestID")
                    ? responseNode.get("CheckoutRequestID").asText()
                    : null;

            payment.setProviderId(checkoutRequestId);
            payment.setMetadata("{\"checkoutRequestId\": \"" + checkoutRequestId + "\", \"customerPhone\": \"" + partyA + "\"}");
            paymentRepository.save(payment);

            return MpesaStkPushResponse.builder()
                    .merchantRequestId(responseNode.has("MerchantRequestID") ? responseNode.get("MerchantRequestID").asText() : null)
                    .checkoutRequestId(checkoutRequestId)
                    .responseCode(responseNode.has("ResponseCode") ? responseNode.get("ResponseCode").asText() : null)
                    .responseDescription(responseNode.has("ResponseDescription") ? responseNode.get("ResponseDescription").asText() : null)
                    .customerMessage(responseNode.has("CustomerMessage") ? responseNode.get("CustomerMessage").asText() : null)
                    .build();

        } catch (Exception e) {
            log.error("STK Push failed: {}", e.getMessage(), e);
            throw new PaymentException("Failed to initiate M-Pesa payment: " + e.getMessage());
        }
    }

    private MpesaStkPushResponse simulateStkPush(Order order, MpesaStkPushRequest request) {
        String checkoutRequestId = "SIM-ws_CO_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        Payment payment = Payment.builder()
                .order(order)
                .provider(Payment.PaymentProvider.MPESA)
                .providerId(checkoutRequestId)
                .amount(request.getAmount())
                .currency("KES")
                .status(Payment.PaymentStatus.PENDING)
                .metadata(com.iloveshopping.service.DataEncryptionService.encryptForJson("{\"checkoutRequestId\": \"" + checkoutRequestId + "\", \"customerPhone\": \"" + request.getPhoneNumber() + "\", \"simulated\": true}"))
                .build();
        paymentRepository.save(payment);

        log.info("Simulated M-Pesa STK Push for order: {} checkoutId: {}", order.getNumber(), checkoutRequestId);

        return MpesaStkPushResponse.builder()
                .merchantRequestId("SIM-" + UUID.randomUUID().toString().substring(0, 8))
                .checkoutRequestId(checkoutRequestId)
                .responseCode("0")
                .responseDescription("Success. Request accepted for processing")
                .customerMessage("An M-Pesa PIN prompt has been sent to your phone.")
                .build();
    }

    @Transactional
    public void processMpesaCallback(String callbackBody) {
        log.info("Processing M-Pesa callback: {}", callbackBody);

        try {
            JsonNode callbackJson = objectMapper.readTree(callbackBody);
            JsonNode stkCallback = callbackJson.get("Body").get("stkCallback");

            String checkoutRequestId = stkCallback.get("CheckoutRequestID").asText();
            int resultCode = stkCallback.get("ResultCode").asInt();
            String resultDesc = stkCallback.get("ResultDesc").asText();

            Optional<Payment> paymentOpt = paymentRepository.findByProviderId(checkoutRequestId);
            if (paymentOpt.isEmpty()) {
                log.warn("Payment not found for checkout request ID: {}", checkoutRequestId);
                return;
            }

            Payment payment = paymentOpt.get();

            if (resultCode == 0) {
                JsonNode callbackMetadata = stkCallback.get("CallbackMetadata").get("Item");
                String amount = callbackMetadata.get(0).get("Value").asText();
                String mpesaReceipt = callbackMetadata.get(1).get("Value").asText();
                String phoneNumber = callbackMetadata.get(4).get("Value").asText();

                payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
                payment.setAmount(new BigDecimal(amount));
                payment.setCallbackData(callbackBody);
                paymentRepository.save(payment);

                Order order = payment.getOrder();
                order.setStatus(Order.OrderStatus.CONFIRMED);
                orderRepository.save(order);

                log.info("M-Pesa payment successful: checkout={}, receipt={}", checkoutRequestId, mpesaReceipt);

            } else {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setCallbackData(callbackBody);
                paymentRepository.save(payment);

                log.warn("M-Pesa payment failed: checkout={}, result={}, desc={}",
                        checkoutRequestId, resultCode, resultDesc);
            }

        } catch (Exception e) {
            log.error("Failed to process M-Pesa callback: {}", e.getMessage(), e);
            throw new PaymentException("Failed to process M-Pesa callback: " + e.getMessage());
        }
    }

    @Transactional
    public void processMpesaTimeout(String timeoutBody) {
        log.warn("M-Pesa timeout: {}", timeoutBody);

        try {
            JsonNode timeoutJson = objectMapper.readTree(timeoutBody);
            String checkoutRequestId = timeoutJson.has("CheckoutRequestID")
                    ? timeoutJson.get("CheckoutRequestID").asText()
                    : null;

            if (checkoutRequestId != null) {
                Optional<Payment> paymentOpt = paymentRepository.findByProviderId(checkoutRequestId);
                if (paymentOpt.isPresent()) {
                    Payment payment = paymentOpt.get();
                    payment.setStatus(Payment.PaymentStatus.FAILED);
                    payment.setCallbackData(timeoutBody);
                    paymentRepository.save(payment);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process M-Pesa timeout: {}", e.getMessage(), e);
        }
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

    public String getPaymentStatus(String checkoutRequestId) {
        try {
            String accessToken = getAccessToken();
            String timestamp = getTimestamp();
            String password = generatePassword(timestamp);

            ObjectNode queryRequest = objectMapper.createObjectNode();
            queryRequest.put("BusinessShortCode", mpesaProperties.getShortcode());
            queryRequest.put("Password", password);
            queryRequest.put("Timestamp", timestamp);
            queryRequest.put("CheckoutRequestID", checkoutRequestId);

            String statusUrl = mpesaProperties.getBaseUrl() + "/mpesa/stkpush/v1/query";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> httpRequest = new HttpEntity<>(objectMapper.writeValueAsString(queryRequest), headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    statusUrl, HttpMethod.POST, httpRequest, JsonNode.class
            );

            JsonNode node = response.getBody();
            return node.has("ResponseCode") ? node.get("ResponseCode").asText() : "UNKNOWN";

        } catch (Exception e) {
            log.error("Failed to query M-Pesa payment status: {}", e.getMessage(), e);
            throw new PaymentException("Failed to query payment status: " + e.getMessage());
        }
    }

    private String getTimestamp() {
        ZonedDateTime kenyaTime = ZonedDateTime.now(ZoneId.of("Africa/Nairobi"));
        return kenyaTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String generatePassword(String timestamp) {
        String data = mpesaProperties.getShortcode() + mpesaProperties.getPasskey() + timestamp;
        return Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }
}