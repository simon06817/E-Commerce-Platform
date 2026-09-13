package com.example.project01.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the order exchange/queue bindings and the JSON message converter
 * used by the outbox publisher and the event listener.
 */
@Configuration
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class RabbitConfig {

    @Bean
    public Queue orderQueue(@Value("${app.rabbitmq.order-queue}") String queue) {
        return new Queue(queue, true);
    }

    @Bean
    public DirectExchange orderExchange(@Value("${app.rabbitmq.order-exchange}") String exchange) {
        return new DirectExchange(exchange, true, false);
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderQueue,
                                       DirectExchange orderExchange,
                                       @Value("${app.rabbitmq.order-routing-key}") String routingKey) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(routingKey);
    }

    @Bean
    public Binding orderPaidBinding(Queue orderQueue,
                                    DirectExchange orderExchange,
                                    @Value("${app.rabbitmq.order-paid-routing-key}") String routingKey) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(routingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
