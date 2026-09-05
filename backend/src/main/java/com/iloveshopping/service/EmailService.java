package com.iloveshopping.service;

import com.iloveshopping.config.AppProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AppProperties appProperties;

    @Async
    public void sendVerificationEmail(String email, String token) {
        log.info("Sending verification email to: {}", email);

        String verificationUrl = appProperties.getFrontendUrl() + "/auth/verify-email?token=" + token;

        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("verificationUrl", verificationUrl);
        context.setVariable("appName", "i-love-shopping");

        sendEmail(email, "Verify Your Email", "email/verification", context);
    }

    @Async
    public void sendPasswordResetEmail(String email, String token) {
        log.info("Sending password reset email to: {}", email);

        String resetUrl = appProperties.getFrontendUrl() + "/auth/reset-password?token=" + token;

        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("resetUrl", resetUrl);
        context.setVariable("appName", "i-love-shopping");

        sendEmail(email, "Password Reset", "email/password-reset", context);
    }

    @Async
    public void sendOrderConfirmation(String email, String orderNumber, String orderId) {
        log.info("Sending order confirmation to: {}", email);

        String orderUrl = appProperties.getFrontendUrl() + "/account/orders/" + orderId;

        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("orderNumber", orderNumber);
        context.setVariable("orderUrl", orderUrl);
        context.setVariable("appName", "i-love-shopping");
        // Items and totals will be set by the caller via overloaded method
        context.setVariable("items", java.util.Collections.emptyList());
        context.setVariable("subtotal", "0.00");
        context.setVariable("tax", "0.00");
        context.setVariable("shipping", "0.00");
        context.setVariable("total", "0.00");
        context.setVariable("shippingAddress", "");

        sendEmail(email, "Order Confirmation - " + orderNumber, "email/order-confirmation", context);
    }

    public void sendOrderConfirmation(com.iloveshopping.entity.Order order) {
        log.info("Sending order confirmation to: {}", order.getGuestEmail() != null ? order.getGuestEmail() : order.getUser().getEmail());

        String email = order.getUser() != null ? order.getUser().getEmail() : order.getGuestEmail();
        if (email == null || email.isBlank()) {
            log.warn("No email for order {}, skipping confirmation", order.getNumber());
            return;
        }
        Context context = orderContext(order, email);
        context.setVariable("paymentProvider", paidVia(order));
        context.setVariable("amountPaid", order.getTotalPaid());

        sendEmail(email, "Order Confirmation - " + order.getNumber(), "email/order-confirmation", context);
    }

    /**
     * Payable invoice for an unpaid order: sent right after checkout and
     * re-sent whenever a payment attempt fails or the STK prompt expires,
     * so the customer can pay later instead of starting over.
     */
    @Async
    public void sendPaymentInvoice(com.iloveshopping.entity.Order order, String reasonLine) {
        String email = order.getUser() != null ? order.getUser().getEmail() : order.getGuestEmail();
        if (email == null || email.isBlank()) {
            log.warn("No email for order {}, skipping invoice", order.getNumber());
            return;
        }
        Context context = orderContext(order, email);
        context.setVariable("payUrl", appProperties.getFrontendUrl() + "/checkout?retry=" + order.getNumber());
        context.setVariable("reasonLine", reasonLine != null ? reasonLine
                : "Your order is awaiting payment.");

        sendEmail(email, "Invoice - pay for order " + order.getNumber(), "email/payment-invoice", context);
    }

    private Context orderContext(com.iloveshopping.entity.Order order, String email) {
        String orderUrl = appProperties.getFrontendUrl() + "/account/orders/" + order.getId();

        java.util.List<java.util.Map<String, Object>> items = new java.util.ArrayList<>();
        for (var item : order.getItems()) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("name", item.getName());
            map.put("quantity", item.getQuantity());
            map.put("price", item.getPrice());
            items.add(map);
        }

        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("orderNumber", order.getNumber());
        context.setVariable("orderUrl", orderUrl);
        context.setVariable("appName", "i-love-shopping");
        context.setVariable("items", items);
        context.setVariable("subtotal", order.getSubtotal());
        context.setVariable("tax", order.getTax());
        context.setVariable("shipping", order.getShipping());
        context.setVariable("total", order.getTotal());
        context.setVariable("shippingAddress", formatAddress(order.getShippingAddress()));
        return context;
    }

    private String paidVia(com.iloveshopping.entity.Order order) {
        return order.getPayments().stream()
                .filter(p -> p.getStatus() == com.iloveshopping.entity.Payment.PaymentStatus.SUCCEEDED)
                .reduce((first, second) -> second)
                .map(p -> p.getProvider().name())
                .orElse("—");
    }

    private String formatAddress(String stored) {
        try {
            String json = DataEncryptionService.decryptStatic(stored);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(json);
            java.util.List<String> lines = new java.util.ArrayList<>();
            addIfPresent(lines, node, "name");
            addIfPresent(lines, node, "line1");
            addIfPresent(lines, node, "line2");
            String city = textOrNull(node, "city");
            String state = textOrNull(node, "state");
            String postal = textOrNull(node, "postalCode");
            String cityLine = java.util.stream.Stream.of(city, state, postal)
                    .filter(s -> s != null && !s.isBlank())
                    .reduce((a, b) -> a + ", " + b).orElse(null);
            if (cityLine != null) lines.add(cityLine);
            addIfPresent(lines, node, "country");
            addIfPresent(lines, node, "phone");
            return lines.isEmpty() ? stored : String.join("<br/>", lines);
        } catch (Exception e) {
            return stored;
        }
    }

    private void addIfPresent(java.util.List<String> lines, com.fasterxml.jackson.databind.JsonNode node, String field) {
        String v = textOrNull(node, field);
        if (v != null && !v.isBlank()) lines.add(v);
    }

    private String textOrNull(com.fasterxml.jackson.databind.JsonNode node, String field) {
        var v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    @Async
    public void send2FASecret(String email, String code) {
        log.info("Sending 2FA code to: {}", email);

        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("code", code);

        sendEmail(email, "Your 2FA Code", "email/2fa-code", context);
    }

    private void sendEmail(String to, String subject, String template, Context context) {
        try {
            String htmlContent = templateEngine.process(template, context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(appProperties.getMailFrom());
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send email: {}", e.getMessage(), e);
            // In development mode, log the email content
            if (appProperties.isDevMode()) {
                log.info("[DEV MODE] Email to: {} | Subject: {}", to, subject);
            }
        }
    }
}