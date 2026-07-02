package com.study.order.producer;

import com.study.common.constant.RabbitMQConstants;
import com.study.common.event.OrderEvent;
import com.study.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 订单消息生产者
 * <p>
 * 演示四种消息发送模式：
 * 1. Direct Exchange - 精确路由
 * 2. Topic Exchange  - 主题路由
 * 3. Fanout Exchange - 广播
 * 4. Work Queue      - 工作队列
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 模式1：Direct Exchange - 精确路由
     * 根据订单状态发送到对应队列
     */
    public void sendOrderEvent(Order order, String routingKey) {
        OrderEvent event = buildOrderEvent(order, routingKey);
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.ORDER_DIRECT_EXCHANGE,
                routingKey,
                event
        );
        log.info("[Direct] 发送订单事件 | 交换机: {} | 路由键: {} | 订单: {}",
                RabbitMQConstants.ORDER_DIRECT_EXCHANGE, routingKey, order.getOrderNo());
    }

    /**
     * 模式2：Topic Exchange - 主题路由
     * 路由键支持通配符，灵活匹配多个队列
     *
     * @param routingKey 如 "order.created", "order.urgent.notify"
     */
    public void sendTopicMessage(Order order, String routingKey) {
        OrderEvent event = buildOrderEvent(order, routingKey);
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.ORDER_TOPIC_EXCHANGE,
                routingKey,
                event
        );
        log.info("[Topic] 发送主题消息 | 交换机: {} | 路由键: {} | 订单: {}",
                RabbitMQConstants.ORDER_TOPIC_EXCHANGE, routingKey, order.getOrderNo());
    }

    /**
     * 模式3：Fanout Exchange - 广播
     * 忽略路由键，发送到所有绑定队列（库存队列、日志队列）
     */
    public void broadcastOrderEvent(Order order) {
        OrderEvent event = buildOrderEvent(order, "broadcast");
        // Fanout 交换机忽略路由键，传空字符串即可
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.ORDER_FANOUT_EXCHANGE,
                "",
                event
        );
        log.info("[Fanout] 广播订单事件 | 订单: {}", order.getOrderNo());
    }

    /**
     * 模式4：Work Queue - 工作队列（竞争消费者）
     * 直接发到队列，多个消费者竞争消费，实现负载均衡
     */
    public void sendToWorkQueue(Order order) {
        OrderEvent event = buildOrderEvent(order, "work");
        // 直接发送到队列（使用默认交换机，路由键=队列名）
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.ORDER_WORK_QUEUE,
                event
        );
        log.info("[Work Queue] 发送到工作队列 | 订单: {}", order.getOrderNo());
    }

    /**
     * 演示：发送带 TTL 的消息（消息级别 TTL）
     * 注意：TTL 过期后消息会进入 DLX
     */
    public void sendWithTtl(Order order, long ttlMillis) {
        OrderEvent event = buildOrderEvent(order, RabbitMQConstants.ROUTING_KEY_ORDER_CREATED);
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.ORDER_DIRECT_EXCHANGE,
                RabbitMQConstants.ROUTING_KEY_ORDER_CREATED,
                event,
                message -> {
                    message.getMessageProperties().setExpiration(String.valueOf(ttlMillis));
                    return message;
                }
        );
        log.info("[TTL] 发送 TTL 消息 | TTL: {}ms | 订单: {}", ttlMillis, order.getOrderNo());
    }

    private OrderEvent buildOrderEvent(Order order, String eventType) {
        return OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .username(order.getUsername())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .amount(order.getAmount())
                .status(OrderEvent.OrderStatus.valueOf(order.getStatus().name()))
                .eventType(eventType)
                .urgent(Boolean.TRUE.equals(order.getUrgent()))
                .remark(order.getRemark())
                .eventTime(LocalDateTime.now())
                .createTime(order.getCreateTime())
                .build();
    }
}
