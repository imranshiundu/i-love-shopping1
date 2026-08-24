package com.iloveshopping.messaging;

import com.iloveshopping.entity.Order;
import com.iloveshopping.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = "order.events";
    private static final String ROUTING_KEY_CREATED = "order.created";
    private static final String ROUTING_KEY_PAID = "order.paid";
    private static final String ROUTING_KEY_CANCELLED = "order.cancelled";

    public void publishOrderCreated(Order order) {
        OrderEvent event = buildEvent(order, OrderEvent.EventType.ORDER_CREATED.name());
        sendEvent(ROUTING_KEY_CREATED, event);
        log.info("Published ORDER_CREATED event for order: {}", order.getNumber());
    }

    public void publishOrderPaid(Order order) {
        OrderEvent event = buildEvent(order, OrderEvent.EventType.ORDER_PAID.name());
        sendEvent(ROUTING_KEY_PAID, event);
        log.info("Published ORDER_PAID event for order: {}", order.getNumber());
    }

    public void publishOrderCancelled(Order order) {
        OrderEvent event = buildEvent(order, OrderEvent.EventType.ORDER_CANCELLED.name());
        sendEvent(ROUTING_KEY_CANCELLED, event);
        log.info("Published ORDER_CANCELLED event for order: {}", order.getNumber());
    }

    private void sendEvent(String routingKey, OrderEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
        } catch (Exception e) {
            log.error("Failed to publish order event to queue '{}': {}", routingKey, e.getMessage(), e);
        }
    }

    private OrderEvent buildEvent(Order order, String eventName) {
        User user = order.getUser();
        return OrderEvent.builder()
                .orderId(order.getId())
                .orderNumber(order.getNumber())
                .userId(user != null ? user.getId() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .eventName(eventName)
                .orderStatus(order.getStatus().name())
                .totalAmount(order.getTotal())
                .currency(order.getCurrency())
                .itemCount(order.getItems() != null ? order.getItems().size() : 0)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
