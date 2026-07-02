package com.study.notification.consumer;

import com.rabbitmq.client.Channel;
import com.study.common.constant.RabbitMQConstants;
import com.study.common.event.OrderEvent;
import com.study.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单消息消费者
 * <p>
 * 演示以下知识点：
 * 1. @RabbitListener 监听指定队列
 * 2. 手动 ACK（basicAck）保证消息可靠消费
 * 3. 消息拒绝（basicNack）+ 死信队列
 * 4. 幂等性处理（通过 eventId 防止重复消费）
 * 5. 竞争消费者（Work Queue）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageConsumer {

    private final NotificationService notificationService;

    // ===================================================================
    // 消费者1：Direct Exchange - 订单创建队列
    // ===================================================================

    /**
     * 消费订单创建消息
     * <p>
     * 演示：手动 ACK + 幂等性处理
     */
    @RabbitListener(queues = RabbitMQConstants.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(OrderEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String eventId = event.getEventId();

        log.info("[Direct-Created] 收到订单创建消息 | 订单: {} | 幂等ID: {}",
                event.getOrderNo(), eventId);

        try {
            // 幂等性检查：防止重复消费
            if (!notificationService.checkAndMarkIdempotent(eventId)) {
                log.warn("[Direct-Created] 重复消息，跳过处理 | eventId: {}", eventId);
                // 重复消息直接 ACK，防止阻塞队列
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 处理业务逻辑
            notificationService.sendEmail(event);

            // 手动 ACK：确认消息已处理（第二个参数 multiple=false 表示只确认当前消息）
            channel.basicAck(deliveryTag, false);
            log.info("[Direct-Created] 消息处理成功，已 ACK | 订单: {}", event.getOrderNo());

        } catch (Exception e) {
            log.error("[Direct-Created] 消息处理失败 | 订单: {} | 异常: {}", event.getOrderNo(), e.getMessage());
            // 手动 NACK：拒绝消息
            // 参数：deliveryTag, multiple=false, requeue=false（不重新入队，转入死信队列）
            channel.basicNack(deliveryTag, false, false);
        }
    }

    // ===================================================================
    // 消费者2：Direct Exchange - 订单支付队列
    // ===================================================================

    @RabbitListener(queues = RabbitMQConstants.ORDER_PAID_QUEUE)
    public void handleOrderPaid(OrderEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("[Direct-Paid] 收到支付成功消息 | 订单: {} | 金额: {}元",
                event.getOrderNo(), event.getAmount());
        try {
            notificationService.sendEmail(event);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[Direct-Paid] 处理失败: {}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    // ===================================================================
    // 消费者3：Direct Exchange - 订单取消队列
    // ===================================================================

    @RabbitListener(queues = RabbitMQConstants.ORDER_CANCELLED_QUEUE)
    public void handleOrderCancelled(OrderEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("[Direct-Cancelled] 收到订单取消消息 | 订单: {}", event.getOrderNo());
        try {
            notificationService.sendEmail(event);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[Direct-Cancelled] 处理失败: {}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    // ===================================================================
    // 消费者4：Topic Exchange - 邮件队列（匹配 order.#）
    // ===================================================================

    /**
     * 消费 Topic 消息（邮件队列）
     * 路由键 "order.#" 匹配以 "order." 开头的所有消息
     */
    @RabbitListener(queues = RabbitMQConstants.ORDER_EMAIL_QUEUE)
    public void handleEmailNotification(OrderEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        log.info("[Topic-Email] 收到消息 | 路由键: {} | 订单: {}", routingKey, event.getOrderNo());
        try {
            notificationService.sendEmail(event);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, true); // requeue=true，重新入队
        }
    }

    // ===================================================================
    // 消费者5：Topic Exchange - 短信队列（匹配 *.urgent.*）
    // ===================================================================

    /**
     * 消费紧急短信通知（只有路由键匹配 *.urgent.* 的消息）
     */
    @RabbitListener(queues = RabbitMQConstants.ORDER_SMS_QUEUE)
    public void handleSmsNotification(OrderEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        log.info("[Topic-SMS] 紧急消息！路由键: {} | 订单: {}", routingKey, event.getOrderNo());
        try {
            notificationService.sendSms(event);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, false);
        }
    }

    // ===================================================================
    // 消费者6：Fanout Exchange - 库存队列（广播接收）
    // ===================================================================

    @RabbitListener(queues = RabbitMQConstants.ORDER_INVENTORY_QUEUE)
    public void handleInventoryUpdate(OrderEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("[Fanout-Inventory] 收到广播消息 | 订单: {}", event.getOrderNo());
        try {
            notificationService.updateInventory(event);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, false);
        }
    }

    // ===================================================================
    // 消费者7：Fanout Exchange - 日志队列（广播接收）
    // ===================================================================

    @RabbitListener(queues = RabbitMQConstants.ORDER_LOG_QUEUE)
    public void handleLogRecord(OrderEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("[Fanout-Log] 收到广播消息 | 订单: {}", event.getOrderNo());
        try {
            notificationService.recordLog(event);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, false);
        }
    }

    // ===================================================================
    // 消费者8：Work Queue - 竞争消费（多个消费者实例竞争处理）
    // ===================================================================

    /**
     * 工作队列消费者（演示竞争消费者模式）
     * 配合 application.yml 中的 concurrency=2，启动2个并发消费者
     */
    @RabbitListener(queues = RabbitMQConstants.ORDER_WORK_QUEUE)
    public void handleWorkTask(OrderEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            notificationService.processWorkTask(event);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[Work Queue] 任务处理失败: {}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    // ===================================================================
    // 消费者9：Dead Letter Queue - 死信队列
    // ===================================================================

    /**
     * 死信队列消费者
     * 处理：消费失败 + TTL过期的消息
     */
    @RabbitListener(queues = RabbitMQConstants.ORDER_DEAD_LETTER_QUEUE)
    public void handleDeadLetter(OrderEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.warn("[Dead Letter] 处理死信消息 | 死因: {} | 订单: {}",
                message.getMessageProperties().getHeaders().get("x-death"),
                event.getOrderNo());
        try {
            notificationService.handleDeadLetter(event);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // 死信队列消息处理失败：直接 ACK 避免死循环
            log.error("[Dead Letter] 死信处理异常，强制 ACK: {}", e.getMessage());
            channel.basicAck(deliveryTag, false);
        }
    }
}
