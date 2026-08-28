package com.iloveshopping.service;

import com.iloveshopping.dto.payment.MpesaStkPushRequest;
import com.iloveshopping.dto.payment.MpesaStkPushResponse;
import com.iloveshopping.dto.payment.PaymentResponse;
import com.iloveshopping.entity.Payment;
import com.iloveshopping.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentService {

    private final MpesaService mpesaService;
    private final PaymentRepository paymentRepository;

    @Transactional
    public MpesaStkPushResponse initiateStkPush(MpesaStkPushRequest request) {
        return mpesaService.initiateStkPush(request);
    }

    public void processMpesaCallback(String callbackBody) {
        mpesaService.processMpesaCallback(callbackBody);
    }

    public void processMpesaTimeout(String timeoutBody) {
        mpesaService.processMpesaTimeout(timeoutBody);
    }

    public MpesaStkPushResponse queryStkStatus(String checkoutRequestId) {
        return mpesaService.queryStkPushStatus(checkoutRequestId);
    }

    public PaymentResponse getPaymentByCheckoutRequestId(String checkoutRequestId) {
        return mpesaService.getPaymentByCheckoutRequestId(checkoutRequestId);
    }

    public List<PaymentResponse> getOrderPayments(String orderNumber) {
        return mpesaService.getOrderPayments(orderNumber);
    }

    public MpesaStkPushResponse retryPayment(String orderNumber) {
        return mpesaService.retryPayment(orderNumber);
    }

    public Page<PaymentResponse> getUserPayments(int page, int size) {
        return paymentRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(PaymentResponse::from);
    }

    public PaymentResponse getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new com.iloveshopping.exception.ResourceNotFoundException("Payment", "id", paymentId));
        return PaymentResponse.from(payment);
    }
}
