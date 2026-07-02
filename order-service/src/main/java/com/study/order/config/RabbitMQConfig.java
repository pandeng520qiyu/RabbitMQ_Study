package com.study.order.config;

import com.study.common.constant.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 配置类
 * <p>
 * 演示以下知识点：
 * 1. Direct Exchange  - 精确路由键匹配
 * 2. Topic Exchange   - 通配符路由（* 匹配一个词，# 匹配零个或多个词）
 * 3. Fanout Exchange  - 广播（忽略路由键，发给所有绑定队列）
 * 4. Dead Letter Queue - 死信队列（消息被拒绝/过期后转移到 DLX）
 * 5. TTL Queue        - 队列级别的消息过期时间
 */
@Configuration
public class RabbitMQConfig {

    // ===================================================================
    // 消息转换器：使用 JSON 序列化消息体
    // ===================================================================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        // 消息发送到交换机失败时触发回调
        template.setMandatory(true);
        template.setReturnsCallback(returned -> {
            System.err.println("[RabbitMQ] 消息路由失败！交换机: " + returned.getExchange()
                + " 路由键: " + returned.getRoutingKey()
                + " 原因: " + returned.getReplyText());
        });
        // 消息到达交换机时触发确认回调
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("[RabbitMQ] 消息未到达交换机！原因: " + cause);
            }
        });
        return template;
    }

    // ===================================================================
    // 1. Direct Exchange - 精确路由
    // 用于：订单创建/支付/取消的精确通知
    // ===================================================================

    @Bean
    public DirectExchange orderDirectExchange() {
        // durable=true 持久化，autoDelete=false 不自动删除
        return new DirectExchange(RabbitMQConstants.ORDER_DIRECT_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCreatedQueue() {
        Map<String, Object> args = new HashMap<>();
        // 设置死信交换机：消息处理失败会进入 DLX
        args.put("x-dead-letter-exchange", RabbitMQConstants.ORDER_DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", RabbitMQConstants.ROUTING_KEY_DEAD_LETTER);
        // 消息过期时间：30分钟（毫秒）
        args.put("x-message-ttl", 1800000);
        return QueueBuilder.durable(RabbitMQConstants.ORDER_CREATED_QUEUE)
                .withArguments(args).build();
    }

    @Bean
    public Queue orderCancelledQueue() {
        return QueueBuilder.durable(RabbitMQConstants.ORDER_CANCELLED_QUEUE).build();
    }

    @Bean
    public Queue orderPaidQueue() {
        return QueueBuilder.durable(RabbitMQConstants.ORDER_PAID_QUEUE).build();
    }

    @Bean
    public Binding bindingOrderCreated() {
        return BindingBuilder.bind(orderCreatedQueue())
                .to(orderDirectExchange())
                .with(RabbitMQConstants.ROUTING_KEY_ORDER_CREATED);
    }

    @Bean
    public Binding bindingOrderCancelled() {
        return BindingBuilder.bind(orderCancelledQueue())
                .to(orderDirectExchange())
                .with(RabbitMQConstants.ROUTING_KEY_ORDER_CANCELLED);
    }

    @Bean
    public Binding bindingOrderPaid() {
        return BindingBuilder.bind(orderPaidQueue())
                .to(orderDirectExchange())
                .with(RabbitMQConstants.ROUTING_KEY_ORDER_PAID);
    }

    // ===================================================================
    // 2. Topic Exchange - 主题路由（通配符匹配）
    // 用于：灵活的消息过滤
    //   * (星号) 匹配一个单词
    //   # (井号) 匹配零个或多个单词
    // 示例：
    //   "order.#"     匹配 "order.created", "order.paid.confirm" 等
    //   "*.urgent.*"  匹配 "order.urgent.notify" 等
    // ===================================================================

    @Bean
    public TopicExchange orderTopicExchange() {
        return new TopicExchange(RabbitMQConstants.ORDER_TOPIC_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderEmailQueue() {
        return QueueBuilder.durable(RabbitMQConstants.ORDER_EMAIL_QUEUE).build();
    }

    @Bean
    public Queue orderSmsQueue() {
        return QueueBuilder.durable(RabbitMQConstants.ORDER_SMS_QUEUE).build();
    }

    /** 邮件队列：匹配所有 order.* 开头的消息 */
    @Bean
    public Binding bindingEmail() {
        return BindingBuilder.bind(orderEmailQueue())
                .to(orderTopicExchange())
                .with("order.#");
    }

    /** 短信队列：只匹配紧急消息 *.urgent.* */
    @Bean
    public Binding bindingSms() {
        return BindingBuilder.bind(orderSmsQueue())
                .to(orderTopicExchange())
                .with("*.urgent.*");
    }

    // ===================================================================
    // 3. Fanout Exchange - 广播
    // 用于：将订单事件广播给所有相关服务（库存/日志/统计等）
    // 特点：忽略路由键，发给所有绑定的队列
    // ===================================================================

    @Bean
    public FanoutExchange orderFanoutExchange() {
        return new FanoutExchange(RabbitMQConstants.ORDER_FANOUT_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderInventoryQueue() {
        return QueueBuilder.durable(RabbitMQConstants.ORDER_INVENTORY_QUEUE).build();
    }

    @Bean
    public Queue orderLogQueue() {
        return QueueBuilder.durable(RabbitMQConstants.ORDER_LOG_QUEUE).build();
    }

    @Bean
    public Binding bindingInventory() {
        return BindingBuilder.bind(orderInventoryQueue()).to(orderFanoutExchange());
    }

    @Bean
    public Binding bindingLog() {
        return BindingBuilder.bind(orderLogQueue()).to(orderFanoutExchange());
    }

    // ===================================================================
    // 4. Dead Letter Queue - 死信队列
    // 消息在以下情况会成为死信：
    //   a. 消息被消费者拒绝（basicReject/basicNack）且 requeue=false
    //   b. 消息 TTL 过期
    //   c. 队列达到最大长度
    // ===================================================================

    @Bean
    public DirectExchange orderDlxExchange() {
        return new DirectExchange(RabbitMQConstants.ORDER_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMQConstants.ORDER_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding bindingDeadLetter() {
        return BindingBuilder.bind(orderDeadLetterQueue())
                .to(orderDlxExchange())
                .with(RabbitMQConstants.ROUTING_KEY_DEAD_LETTER);
    }

    // ===================================================================
    // 5. Work Queue - 工作队列（竞争消费者模式）
    // 用于：任务分发，多个消费者竞争消费，实现负载均衡
    // ===================================================================

    @Bean
    public Queue orderWorkQueue() {
        return QueueBuilder.durable(RabbitMQConstants.ORDER_WORK_QUEUE).build();
    }
}
