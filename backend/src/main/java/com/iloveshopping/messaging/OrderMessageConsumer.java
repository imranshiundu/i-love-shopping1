package com.iloveshopping.messaging;

import com.iloveshopping.entity.Order;
import com.iloveshopping.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderMessageConsumer {

    private final OrderRepository orderRepository;

    @RabbitListener(queues = "order.created")
    public void handleOrderCreated(OrderEvent event) {
        log.info("Received ORDER_CREATED event for order: {} (user: {})",
                event.getOrderNumber(), event.getUserEmail());

        try {
            orderRepository.findById(event.getOrderId()).ifPresent(order -> {
                if (order.getStatus() == Order.OrderStatus.PENDING) {
                    log.info("Order {} created and awaiting payment", event.getOrderNumber());
                }
            });
        } catch (Exception e) {
            log.error("Failed to process ORDER_CREATED event for order {}: {}",
                    event.getOrderNumber(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "order.paid")
    public void handleOrderPaid(OrderEvent event) {
        log.info("Received ORDER_PAID event for order: {} (amount: {} {})",
                event.getOrderNumber(), event.getTotalAmount(), event.getCurrency());

        try {
            orderRepository.findById(event.getOrderId()).ifPresent(order -> {
                if (order.getStatus() == Order.OrderStatus.PENDING) {
                    order.setStatus(Order.OrderStatus.CONFIRMED);
                    orderRepository.save(order);
                    log.info("Order {} status updated to CONFIRMED after payment", event.getOrderNumber());
                }
            });
        } catch (Exception e) {
            log.error("Failed to process ORDER_PAID event for order {}: {}",
                    event.getOrderNumber(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "order.cancelled")
    public void handleOrderCancelled(OrderEvent event) {
        log.info("Received ORDER_CANCELLED event for order: {} (user: {})",
                event.getOrderNumber(), event.getUserEmail());

        try {
            orderRepository.findById(event.getOrderId()).ifPresent(order -> {
                if (order.canBeCancelled()) {
                    order.setStatus(Order.OrderStatus.CANCELLED);
                    orderRepository.save(order);
                    log.info("Order {} cancelled via event", event.getOrderNumber());
                }
            });
        } catch (Exception e) {
            log.error("Failed to process ORDER_CANCELLED event for order {}: {}",
                    event.getOrderNumber(), e.getMessage(), e);
        }
    }
}
