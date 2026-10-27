package com.iloveshopping.service;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Central business-metrics registry. All custom counters live here so the
 * observable surface stays discoverable: Prometheus scrapes
 * /actuator/prometheus on the management port (9090, internal network only).
 *
 * Thread safety: untagged counters are created once and shared; tagged
 * counters go through MeterRegistry.counter(name, tags...) on every call —
 * the registry caches by id, so repeated lookups are cheap and no stateful
 * builder is ever reused across threads.
 *
 * Cardinality discipline: labels are low-cardinality poles (provider,
 * reason, rating), never user/session ids.
 */
@Service
public class AppMetrics {

    private final MeterRegistry registry;

    private final Counter usersRegistered;
    private final Counter passwordResetRequested;
    private final Counter twoFaFailed;
    private final Counter twoFaEnrolled;
    private final Counter ordersCreated;
    private final Counter ordersPaid;
    private final Counter ordersCancelled;
    private final Counter ordersRefunded;
    private final Counter revenueKes;
    private final Counter cartItemAdded;
    private final Counter searchSuggestions;
    private final Counter refreshSuccess;

    public AppMetrics(MeterRegistry registry) {
        this.registry = registry;
        usersRegistered = counter("ils_users_registered_total", "New user registrations");
        passwordResetRequested = counter("ils_auth_password_reset_total", "Password reset requests");
        twoFaFailed = counter("ils_auth_2fa_failed_total", "Rejected 2FA codes");
        twoFaEnrolled = counter("ils_auth_2fa_enrolled_total", "2FA enrollments completed");
        ordersCreated = counter("ils_orders_placed_total", "Orders placed (checkout)");
        ordersPaid = counter("ils_orders_paid_total", "Orders with a verified payment");
        ordersCancelled = counter("ils_orders_cancelled_total", "Orders cancelled");
        ordersRefunded = counter("ils_orders_refunded_total", "Orders refunded");
        revenueKes = counter("ils_revenue_kes_total", "Revenue collected, in KES");
        cartItemAdded = counter("ils_cart_items_added_total", "Items added to carts");
        searchSuggestions = counter("ils_search_suggestion_requests_total", "Search suggestions served");
        refreshSuccess = counter("ils_auth_refresh_success_total", "JWT refresh tokens rotated successfully");
    }

    /** Enables @Timed annotations on service methods. */
    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry);
    }

    public void userRegistered()                { usersRegistered.increment(); }
    public void passwordResetRequested()       { passwordResetRequested.increment(); }
    public void twoFaFailed()                   { twoFaFailed.increment(); }
    public void twoFaEnrolled()                 { twoFaEnrolled.increment(); }

    public void loginSuccess(String mode)       { registry.counter("ils_auth_login_success_total", "mode", mode).increment(); }
    public void refreshSuccess()                { refreshSuccess.increment(); }
    public void refreshFailed(String reason)    { registry.counter("ils_auth_refresh_failed_total", "reason", reason).increment(); }
    public void loginFailed(String reason)      { registry.counter("ils_auth_login_failed_total", "reason", reason).increment(); }

    public void orderCreated()                  { ordersCreated.increment(); }
    public void orderCancelled()                { ordersCancelled.increment(); }
    public void orderRefunded()                 { ordersRefunded.increment(); }

    public void orderPaid(BigDecimal amount) {
        ordersPaid.increment();
        revenueKes.increment(amount.doubleValue());
    }

    public void paymentFailed(String provider, String reason) {
        registry.counter("ils_payment_failed_total", "provider", provider, "reason", reason).increment();
    }

    public void reviewCreated(int rating)       { registry.counter("ils_reviews_created_total", "rating", String.valueOf(rating)).increment(); }
    public void cartItemAdded()                 { cartItemAdded.increment(); }
    public void searchSuggestionRequested()     { searchSuggestions.increment(); }
    public void rateLimited(String scope)       { registry.counter("ils_rate_limited_total", "scope", scope).increment(); }

    private Counter counter(String name, String description) {
        return Counter.builder(name).description(description).register(registry);
    }
}
