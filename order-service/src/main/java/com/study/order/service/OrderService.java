package com.study.order.service;

import com.study.common.constant.RabbitMQConstants;
import com.study.order.entity.Order;
import com.study.order.producer.OrderMessageProducer;
import com.study.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * 订单业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMessageProducer messageProducer;

    @Transactional
    public Order createOrder(Order order) {
        // 生成订单号
        order.setOrderNo(generateOrderNo());
        order.setStatus(Order.OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(order);

        // ① Direct Exchange：发送订单创建事件
        messageProducer.sendOrderEvent(savedOrder, RabbitMQConstants.ROUTING_KEY_ORDER_CREATED);

        // ② Topic Exchange：发送主题消息（邮件队列 order.# 可接收）
        String topicKey = Boolean.TRUE.equals(order.getUrgent())
                ? RabbitMQConstants.ROUTING_KEY_ORDER_URGENT   // *.urgent.* -> 短信也接收
                : RabbitMQConstants.ROUTING_KEY_ORDER_CREATED; // order.# -> 只有邮件接收
        messageProducer.sendTopicMessage(savedOrder, topicKey);

        // ③ Fanout Exchange：广播给库存/日志服务
        messageProducer.broadcastOrderEvent(savedOrder);

        // ④ Work Queue：分发给工作队列处理
        messageProducer.sendToWorkQueue(savedOrder);

        log.info("订单创建成功，已发送 4 种消息模式 | 订单号: {}", savedOrder.getOrderNo());
        return savedOrder;
    }

    @Transactional
    public Order payOrder(Long orderId) {
        Order order = getOrderOrThrow(orderId);
        order.setStatus(Order.OrderStatus.PAID);
        order = orderRepository.save(order);

        // Direct Exchange：支付成功通知
        messageProducer.sendOrderEvent(order, RabbitMQConstants.ROUTING_KEY_ORDER_PAID);
        log.info("订单已支付 | 订单号: {}", order.getOrderNo());
        return order;
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = getOrderOrThrow(orderId);
        order.setStatus(Order.OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        // Direct Exchange：取消通知
        messageProducer.sendOrderEvent(order, RabbitMQConstants.ROUTING_KEY_ORDER_CANCELLED);
        log.info("订单已取消 | 订单号: {}", order.getOrderNo());
        return order;
    }

    /**
     * 演示：发送带 TTL 的消息（5秒后过期，进入死信队列）
     */
    @Transactional
    public Order createOrderWithTtl(Order order) {
        order.setOrderNo(generateOrderNo());
        order.setStatus(Order.OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(order);

        // 5秒 TTL，消息过期后进入死信队列
        messageProducer.sendWithTtl(savedOrder, 5000L);
        log.info("创建 TTL 订单（5秒后消息过期进入死信队列）| 订单号: {}", savedOrder.getOrderNo());
        return savedOrder;
    }

    public Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderId));
    }

    public List<Order> listOrders() {
        return orderRepository.findAll();
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = new Random().nextInt(9000) + 1000;
        return "ORD" + timestamp + random;
    }
}
