package com.iloveshopping.cart;

import com.iloveshopping.entity.Cart;
import com.iloveshopping.entity.CartItem;
import com.iloveshopping.entity.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CartFunctionalityTest {

    @Test
    void cartTotalCalculationIsAccurate() {
        Product p1 = Product.builder().name("A").slug("a").description("d").price(new BigDecimal("100.00")).sku("A-1").stock(10).build();
        Product p2 = Product.builder().name("B").slug("b").description("d").price(new BigDecimal("250.50")).sku("B-1").stock(10).build();
        Cart cart = Cart.builder().build();
        CartItem i1 = CartItem.builder().cart(cart).product(p1).quantity(2).priceSnapshot(p1.getPrice()).build();
        CartItem i2 = CartItem.builder().cart(cart).product(p2).quantity(1).priceSnapshot(p2.getPrice()).build();
        cart.setItems(List.of(i1, i2));
        // subtotal = 100*2 + 250.50*1 = 450.50
        assertEquals(0, new BigDecimal("450.50").compareTo(cart.getSubtotal()));
        assertEquals(3, cart.getTotalItems());
    }

    @Test
    void cartLineTotalReflectsPriceSnapshot() {
        Product p = Product.builder().name("A").slug("a").description("d").price(new BigDecimal("89.00")).sku("A-1").stock(20).build();
        CartItem item = CartItem.builder().quantity(3).priceSnapshot(p.getPrice()).product(p).build();
        assertEquals(0, new BigDecimal("267.00").compareTo(item.getLineTotal()));
    }

    @Test
    void cartHandlesEmptyItems() {
        Cart cart = Cart.builder().build();
        assertEquals(0, BigDecimal.ZERO.compareTo(cart.getSubtotal()));
        assertEquals(0, cart.getTotalItems());
    }

    @Test
    void cartUpdateQuantityRecalculates() {
        Product p = Product.builder().name("A").slug("a").description("d").price(new BigDecimal("50.00")).sku("A-1").stock(100).build();
        CartItem item = CartItem.builder().quantity(1).priceSnapshot(p.getPrice()).product(p).build();
        assertEquals(0, new BigDecimal("50.00").compareTo(item.getLineTotal()));
        item.setQuantity(5);
        assertEquals(0, new BigDecimal("250.00").compareTo(item.getLineTotal()));
    }
}
