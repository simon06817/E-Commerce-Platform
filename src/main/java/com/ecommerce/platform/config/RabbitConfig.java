package com.ecommerce.platform.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

/** RabbitMQ 配置。 */
@Configuration
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
@Slf4j
public class RabbitConfig {

    /** 创建订单队列。 */
    @Bean
    public Queue orderQueue(@Value("${app.rabbitmq.order-queue}") String queue,
                            @Value("${app.rabbitmq.order-dead-letter-exchange}") String deadLetterExchange,
                            @Value("${app.rabbitmq.order-dead-letter-routing-key}") String deadLetterRoutingKey) {
        return QueueBuilder.durable(queue)
                .withArgument("x-dead-letter-exchange", deadLetterExchange)
                .withArgument("x-dead-letter-routing-key", deadLetterRoutingKey)
                .build();
    }

    /** 创建订单交换机。 */
    @Bean
    public DirectExchange orderExchange(@Value("${app.rabbitmq.order-exchange}") String exchange) {
        return new DirectExchange(exchange, true, false);
    }

    /** 绑定订单创建事件路由。 */
    @Bean
    public Binding orderCreatedBinding(Queue orderQueue,
                                       DirectExchange orderExchange,
                                       @Value("${app.rabbitmq.order-routing-key}") String routingKey) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(routingKey);
    }

    /** 绑定订单支付事件路由。 */
    @Bean
    public Binding orderPaidBinding(Queue orderQueue,
                                    DirectExchange orderExchange,
                                    @Value("${app.rabbitmq.order-paid-routing-key}") String routingKey) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(routingKey);
    }

    /** 创建订单死信队列。 */
    @Bean
    public Queue orderDeadLetterQueue(@Value("${app.rabbitmq.order-dead-letter-queue}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    /** 创建订单死信交换机。 */
    @Bean
    public DirectExchange orderDeadLetterExchange(
            @Value("${app.rabbitmq.order-dead-letter-exchange}") String exchange) {
        return new DirectExchange(exchange, true, false);
    }

    /** 绑定订单死信队列路由。 */
    @Bean
    public Binding orderDeadLetterBinding(
            Queue orderDeadLetterQueue,
            DirectExchange orderDeadLetterExchange,
            @Value("${app.rabbitmq.order-dead-letter-routing-key}") String routingKey) {
        return BindingBuilder.bind(orderDeadLetterQueue).to(orderDeadLetterExchange).with(routingKey);
    }

    /** 创建消息转换器。 */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** 创建 RabbitMQ 消息模板。 */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            String id = correlationData == null ? "unknown" : correlationData.getId();
            if (ack) {
                log.debug("RabbitMQ publish confirmed, correlationId={}", id);
            } else {
                log.error("RabbitMQ publish not confirmed, correlationId={}, cause={}", id, cause);
            }
        });
        template.setReturnsCallback(returned -> log.error(
                "RabbitMQ message returned, replyCode={}, replyText={}, exchange={}, routingKey={}",
                returned.getReplyCode(),
                returned.getReplyText(),
                returned.getExchange(),
                returned.getRoutingKey()));
        return template;
    }
}
