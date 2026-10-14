package com.iloveshopping.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.aop.TimedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.function.Supplier;

/**
 * Central business-metrics registry. All custom counters live here so the
 * observable surface stays discoverable: Prometheus endpoint at
 * /actuator/prometheus (management port 9090, internal network only).
 *
 * Cardinality discipline: labels are low-cardinality poles (provider, reason,
 * rating band), never user/session ids — storage stays cheap and queries fast.
 */
@Service
public class AppMetrics {

    private final MeterRegistry registry;
    private final Counter.Builder usersRegistered;
    private final Counter.Builder loginSuccess;
    private final Counter.Builder loginFailed;
    private final Counter.Builder passwordResetRequested;
    private final Counter.Builder twoFaFailed;
    private final Counter.Builder twoFaEnrolled;
    private final Counter.Builder ordersCreated;
    private final Counter.Builder ordersPaid;
    private final Counter.Builder ordersCancelled;
    private final Counter.Builder ordersRefunded;
    private final Counter.Builder paymentFailed;
    private final Counter.Builder reviewsCreated;
    private final Counter.Builder cartItemAdded;
    private final Counter.Builder searchSuggestions;

        @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry);
    }

    public AppMetrics(MeterRegistry registry) {
        this.registry = registry;
        usersRegistered = Counter.builder("ils_users_registered_total").description("New user registrations");
        loginSuccess = Counter.builder("ils_auth_login_success_total").description("Successful logins").tag("mode", "unknown");
        loginFailed = Counter.builder("ils_auth_login_failed_total").description("Failed login attempts").tag("reason", "unknown");
        passwordResetRequested = Counter.builder("ils_auth_password_reset_total").description("Password reset requests");
        twoFaFailed = Counter.builder("ils_auth_2fa_failed_total").description("Rejected 2FA codes");
        twoFaEnrolled = Counter.builder("ils_auth_2fa_enrolled_total").description("2FA enrollments completed");
        ordersCreated = Counter.builder("ils_orders_created_total").description("Orders placed (checkout)");
        ordersPaid = Counter.builder("ils_orders_paid_total").description("Orders with a verified payment");
        ordersCancelled = Counter.builder("ils_orders_cancelled_total").description("Orders cancelled");
        ordersRefunded = Counter.builder("ils_orders_refunded_total").description("Orders refunded");
        paymentFailed = Counter.builder("ils_payment_failed_total").description("Failed payment attempts").tag("provider", "?").tag("reason", "?");
        reviewsCreated = Counter.builder("ils_reviews_created_total").description("Reviews submitted").tag("rating", "0");
        cartItemAdded = Counter.builder("ils_cart_items_added_total").description("Items added to carts");
        searchSuggestions = Counter.builder("ils_search_suggestion_requests_total").description("Search suggestions served");
    }

    public void userRegistered()              { usersRegistered.register(registry).increment(); }
    public void userRegistered(String source) { usersRegistered.tag("source", source).register(registry).increment(); }

    public void loginSuccess(String mode)     { loginSuccess.tag("mode", mode).register(registry).increment(); }
    public void loginFailed(String reason)    { loginFailed.tag("reason", reason).register(registry).increment(); }

    public void passwordResetRequested()      { passwordResetRequested.register(registry).increment(); }
    public void twoFaFailed()                 { twoFaFailed.register(registry).increment(); }
    public void twoFaEnrolled()               { twoFaEnrolled.register(registry).increment(); }

    public void orderCreated()                { ordersCreated.register(registry).increment(); }
    public void orderPaid(BigDecimal amount)  { ordersPaid.register(registry).increment(); registry.counter("ils_revenue_kes_total").increment(amount.doubleValue()); }
    public void orderCancelled()              { ordersCancelled.register(registry).increment(); }
    public void orderRefunded()               { ordersRefunded.register(registry).increment(); }

    public void paymentFailed(String provider, String reason) {
        paymentFailed.tag("provider", provider).tag("reason", reason).register(registry).increment();
    }

    public void reviewCreated(int rating)     { reviewsCreated.tag("rating", String.valueOf(rating)).register(registry).increment(); }
    public void cartItemAdded()               { cartItemAdded.register(registry).increment(); }

    public void searchSuggestionRequested()   { searchSuggestions.register(registry).increment(); }

    public void rateLimited(String scope)     { registry.counter("ils_rate_limited_total", "scope", scope).increment(); }

    /** Time a method call — for the heavier read paths. */
    public <T> T time(String name, Supplier<T> block) {
        return Timer.builder("ils_query_seconds").tag("name", name).register(registry).record(block);
    }
}
