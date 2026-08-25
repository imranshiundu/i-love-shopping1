package com.iloveshopping.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "order.events";
    public static final String QUEUE_CREATED = "order.created";
    public static final String QUEUE_PAID = "order.paid";
    public static final String QUEUE_CANCELLED = "order.cancelled";
    public static final String DLX = "order.dlx";
    public static final String DLQ = "order.dead-letter";

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLQ);
    }

    private Queue withDlq(String name) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    public Queue orderCreatedQueue() {
        return withDlq(QUEUE_CREATED);
    }

    @Bean
    public Queue orderPaidQueue() {
        return withDlq(QUEUE_PAID);
    }

    @Bean
    public Queue orderCancelledQueue() {
        return withDlq(QUEUE_CANCELLED);
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(orderExchange).with(QUEUE_CREATED);
    }

    @Bean
    public Binding orderPaidBinding(Queue orderPaidQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderPaidQueue).to(orderExchange).with(QUEUE_PAID);
    }

    @Bean
    public Binding orderCancelledBinding(Queue orderCancelledQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderCancelledQueue).to(orderExchange).with(QUEUE_CANCELLED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
