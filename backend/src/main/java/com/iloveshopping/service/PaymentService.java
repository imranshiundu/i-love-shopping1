package com.iloveshopping.service;

import com.iloveshopping.dto.payment.MpesaStkPushRequest;
import com.iloveshopping.dto.payment.MpesaStkPushResponse;
import com.iloveshopping.dto.payment.PaymentResponse;
import com.iloveshopping.entity.Payment;
import com.iloveshopping.exception.ResourceNotFoundException;
import com.iloveshopping.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final MpesaService mpesaService;
    private final PaymentRepository paymentRepository;

    @Transactional
    public MpesaStkPushResponse initiateMpesaStkPush(MpesaStkPushRequest request) {
        return mpesaService.initiateStkPush(request);
    }

    @Transactional
    public void processMpesaCallback(String body) {
        mpesaService.processMpesaCallback(body);
    }

    @Transactional
    public void processMpesaTimeout(String body) {
        mpesaService.processMpesaTimeout(body);
    }

    public MpesaStkPushResponse queryMpesaStkStatus(String checkoutRequestId) {
        return mpesaService.queryStkStatus(checkoutRequestId);
    }

    @Transactional
    public MpesaStkPushResponse retryMpesaPayment(String orderNumber, String phoneNumber) {
        return mpesaService.retryPayment(orderNumber, phoneNumber);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByCheckoutRequestId(String checkoutRequestId) {
        Payment p = paymentRepository.findByProviderId(checkoutRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + checkoutRequestId));
        return PaymentResponse.from(p);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String paymentId) {
        Payment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        return PaymentResponse.from(p);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> listPayments(int page, int size) {
        return paymentRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)))
                .map(PaymentResponse::from);
    }
}
