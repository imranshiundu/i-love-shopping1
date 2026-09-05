package com.iloveshopping.controller;

import com.iloveshopping.exception.GlobalExceptionHandler;
import com.iloveshopping.service.OrderService;
import com.iloveshopping.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API tests for unpaid-order self-service: delete delegates to the service
 * layer which enforces the unpaid-only rule.
 */
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrderController(orderService, paymentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void deleteUnpaid_delegatesToService() throws Exception {
        mockMvc.perform(delete("/orders/ILS-123"))
                .andExpect(status().isOk());

        verify(orderService).deleteUnpaidOrder(eq("ILS-123"), any());
    }
}
