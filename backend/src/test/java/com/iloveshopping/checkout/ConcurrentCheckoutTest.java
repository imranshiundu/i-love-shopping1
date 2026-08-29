package com.iloveshopping.checkout;

import com.iloveshopping.entity.Order;
import com.iloveshopping.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates the business rules for checkout and order creation.
 * Integration tests for atomic stock decrement are covered in
 * the live deployment verification scripts.
 */
class CheckoutValidationTest {

    @Test
    void insufficientStockExceptionCarriesDetails() {
        InsufficientStockException ex = new InsufficientStockException("Test Mug", 10, 3);
        assertEquals("Test Mug", ex.getProductName());
        assertEquals(10, ex.getRequested());
        assertEquals(3, ex.getAvailable());
        assertTrue(ex.getMessage().contains("Test Mug"));
        assertTrue(ex.getMessage().contains("10"));
        assertTrue(ex.getMessage().contains("3"));
    }

    @Test
    void orderExposesTotalAndStatus() {
        Order order = Order.builder()
                .number("ILS-1234")
                .status(Order.OrderStatus.PENDING)
                .subtotal(new BigDecimal("1000.00"))
                .tax(new BigDecimal("160.00"))
                .shipping(new BigDecimal("200.00"))
                .total(new BigDecimal("1360.00"))
                .build();
        assertEquals(new BigDecimal("1360.00"), order.getTotal());
        assertEquals(Order.OrderStatus.PENDING, order.getStatus());
        assertFalse(order.isFullyPaid()); // no payments
    }

    @Test
    void orderBecomesFullyPaidWhenSucceededPaymentEqualsTotal() {
        Order order = Order.builder()
                .number("ILS-9999")
                .status(Order.OrderStatus.CONFIRMED)
                .subtotal(new BigDecimal("1000"))
                .shipping(BigDecimal.ZERO)
                .tax(BigDecimal.ZERO)
                .total(new BigDecimal("1000"))
                .build();
        com.iloveshopping.entity.Payment p = com.iloveshopping.entity.Payment.builder()
                .order(order)
                .provider(com.iloveshopping.entity.Payment.PaymentProvider.MPESA)
                .providerId("test-1")
                .amount(new BigDecimal("1000"))
                .status(com.iloveshopping.entity.Payment.PaymentStatus.SUCCEEDED)
                .build();
        order.getPayments().add(p);
        assertTrue(order.isFullyPaid());
        assertEquals(0, new BigDecimal("1000").compareTo(order.getTotalPaid()));
    }
}
