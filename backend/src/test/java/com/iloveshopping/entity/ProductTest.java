package com.iloveshopping.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void shouldCreateProductWithCorrectDefaults() {
        Product product = Product.builder()
                .name("Test Product")
                .slug("test-product")
                .description("Test description")
                .price(new BigDecimal("99.99"))
                .sku("TEST-001")
                .stock(10)
                .isActive(true)
                .build();

        assertEquals("Test Product", product.getName());
        assertEquals("test-product", product.getSlug());
        assertEquals(BigDecimal.valueOf(99.99), product.getPrice());
        assertEquals("TEST-001", product.getSku());
        assertEquals(10, product.getStock());
        assertTrue(product.getIsActive());
        assertNull(product.getCompareAtPrice());
    }

    @Test
    void shouldIdentifyOnSaleProducts() {
        Product product = Product.builder()
                .name("Sale Product")
                .slug("sale-product")
                .description("Description")
                .price(new BigDecimal("79.99"))
                .compareAtPrice(new BigDecimal("99.99"))
                .sku("SALE-001")
                .stock(5)
                .build();

        assertTrue(product.isOnSale());
        assertEquals(20, product.getDiscountPercentage());
    }

    @Test
    void shouldNotMarkRegularPriceAsOnSale() {
        Product product = Product.builder()
                .name("Regular Product")
                .slug("regular-product")
                .description("Description")
                .price(new BigDecimal("50.00"))
                .sku("REG-001")
                .stock(5)
                .build();

        assertFalse(product.isOnSale());
        assertEquals(0, product.getDiscountPercentage());
    }

    @Test
    void shouldNotMarkOnSaleWhenCompareAtPriceIsLower() {
        Product product = Product.builder()
                .name("Wrong Pricing")
                .slug("wrong-pricing")
                .description("Description")
                .price(new BigDecimal("99.99"))
                .compareAtPrice(new BigDecimal("49.99"))
                .sku("WRONG-001")
                .stock(5)
                .build();

        assertFalse(product.isOnSale());
        assertEquals(0, product.getDiscountPercentage());
    }

    @Test
    void shouldIdentifyOutOfStockProducts() {
        Product product = Product.builder()
                .name("Out of Stock")
                .slug("out-of-stock")
                .description("Description")
                .price(new BigDecimal("10.00"))
                .sku("OOS-001")
                .stock(0)
                .build();

        assertFalse(product.isInStock());
    }

    @Test
    void shouldIdentifyInStockProducts() {
        Product product = Product.builder()
                .name("In Stock")
                .slug("in-stock")
                .description("Description")
                .price(new BigDecimal("10.00"))
                .sku("INSTOCK-001")
                .stock(10)
                .build();

        assertTrue(product.isInStock());
    }

    @Test
    void shouldReturnEffectivePrice() {
        Product product = Product.builder()
                .name("Product")
                .slug("product")
                .description("Description")
                .price(new BigDecimal("49.99"))
                .compareAtPrice(new BigDecimal("99.99"))
                .sku("EFF-001")
                .build();

        assertEquals(new BigDecimal("49.99"), product.getEffectivePrice());
    }

    @Test
    void shouldCalculateDiscountPercentageCorrectly() {
        Product product = Product.builder()
                .name("Discounted Product")
                .slug("discounted")
                .description("Description")
                .price(new BigDecimal("75.00"))
                .compareAtPrice(new BigDecimal("100.00"))
                .sku("DISC-001")
                .build();

        assertEquals(25, product.getDiscountPercentage());
    }

    @Test
    void shouldHandleZeroStock() {
        Product product = Product.builder()
                .name("Zero Stock")
                .slug("zero-stock")
                .description("Description")
                .price(BigDecimal.TEN)
                .sku("ZERO-001")
                .stock(0)
                .build();

        assertEquals(0, product.getStock());
        assertFalse(product.isInStock());
    }

    @Test
    void shouldHandleLargeStockValues() {
        Product product = Product.builder()
                .name("Bulk Product")
                .slug("bulk-product")
                .description("Description")
                .price(BigDecimal.ONE)
                .sku("BULK-001")
                .stock(999999)
                .build();

        assertEquals(999999, product.getStock());
        assertTrue(product.isInStock());
    }
}