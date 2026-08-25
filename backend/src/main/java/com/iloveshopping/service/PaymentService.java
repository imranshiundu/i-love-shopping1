package com.iloveshopping.service;

import com.iloveshopping.dto.payment.MpesaStkPushRequest;
import com.iloveshopping.dto.payment.MpesaStkPushResponse;
import com.iloveshopping.dto.payment.PaymentResponse;
import com.iloveshopping.entity.Payment;
import com.iloveshopping.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public List<PaymentResponse> getOrderPayments(String orderNumber) {
        return mpesaService.getOrderPayments(orderNumber);
    }

    public MpesaStkPushResponse retryPayment(String orderNumber) {
        return mpesaService.retryPayment(orderNumber);
    }

    public String getPaymentStatus(String checkoutRequestId) {
        return mpesaService.getPaymentStatus(checkoutRequestId);
    }
}