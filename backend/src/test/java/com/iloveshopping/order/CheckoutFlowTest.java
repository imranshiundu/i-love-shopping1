package com.iloveshopping.order;

import com.iloveshopping.config.AppProperties;
import com.iloveshopping.entity.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutFlowTest {

    @Test
    void orderSummaryCalculationsArePrecise() {
        AppProperties props = new AppProperties();
        props.setTaxRate(0.16);
        props.setFreeShippingThreshold(5000);
        BigDecimal subtotal = new BigDecimal("1200.00");
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(props.getTaxRate()));
        BigDecimal shipping = subtotal.compareTo(BigDecimal.valueOf(props.getFreeShippingThreshold())) >= 0
                ? BigDecimal.ZERO : BigDecimal.valueOf(200);
        BigDecimal total = subtotal.add(tax).add(shipping);
        // 1200 + 192 + 200 = 1592
        assertEquals(0, new BigDecimal("192.00").compareTo(tax));
        assertEquals(0, BigDecimal.valueOf(200).compareTo(shipping));
        assertEquals(0, new BigDecimal("1592.00").compareTo(total));
    }

    @Test
    void freeShippingAboveThreshold() {
        AppProperties props = new AppProperties();
        props.setFreeShippingThreshold(5000);
        BigDecimal subtotal = new BigDecimal("5000.00");
        BigDecimal shipping = subtotal.compareTo(BigDecimal.valueOf(props.getFreeShippingThreshold())) >= 0
                ? BigDecimal.ZERO : BigDecimal.valueOf(200);
        assertEquals(0, BigDecimal.ZERO.compareTo(shipping));
    }

    @Test
    void orderCanBeCancelledWhenPending() {
        Order order = Order.builder().status(Order.OrderStatus.PENDING).total(new BigDecimal("1000")).subtotal(new BigDecimal("800")).shipping(BigDecimal.ZERO).tax(BigDecimal.ZERO).build();
        assertTrue(order.canBeCancelled());
        order.setStatus(Order.OrderStatus.SHIPPED);
        assertFalse(order.canBeCancelled());
    }

    @Test
    void guestOrderHasNoUserButGuestEmail() {
        Order order = Order.builder().user(null).guestEmail("guest@example.com").status(Order.OrderStatus.PENDING).total(new BigDecimal("500")).subtotal(new BigDecimal("400")).shipping(BigDecimal.ZERO).tax(BigDecimal.ZERO).build();
        assertNull(order.getUser());
        assertEquals("guest@example.com", order.getGuestEmail());
    }

    @Test
    void expiredOrdersCannotBeCancelled() {
        Order order = Order.builder().status(Order.OrderStatus.EXPIRED).total(new BigDecimal("1000")).subtotal(new BigDecimal("800")).shipping(BigDecimal.ZERO).tax(BigDecimal.ZERO).build();
        assertFalse(order.canBeCancelled());
    }
}
