package com.study.notification.service;

import com.study.common.event.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知发送服务（模拟）
 * <p>
 * 包含幂等性处理：通过 eventId 防止重复消费
 */
@Slf4j
@Service
public class NotificationService {

    /**
     * 简单的内存去重集合（生产环境应使用 Redis）
     */
    private final Set<String> processedEventIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * 检查并标记 eventId（幂等性控制）
     *
     * @return true=首次处理，false=重复消息
     */
    public boolean checkAndMarkIdempotent(String eventId) {
        return processedEventIds.add(eventId);
    }

    /**
     * 发送邮件通知（模拟）
     */
    public void sendEmail(OrderEvent event) {
        log.info("📧 [邮件通知] 发送给用户: {} | 订单: {} | 状态: {} | 金额: {}元",
                event.getUsername(),
                event.getOrderNo(),
                event.getStatus(),
                event.getAmount());
        // 实际应用中接入 JavaMailSender 或第三方邮件服务
    }

    /**
     * 发送短信通知（模拟）
     */
    public void sendSms(OrderEvent event) {
        log.info("📱 [短信通知] 紧急消息 → 用户: {} | 订单: {} | 内容: 您的订单 {} 需要紧急处理！",
                event.getUsername(),
                event.getOrderNo(),
                event.getOrderNo());
        // 实际应用中接入阿里云短信 SDK 等
    }

    /**
     * 更新库存（模拟）
     */
    public void updateInventory(OrderEvent event) {
        log.info("📦 [库存服务] 商品: {} | 数量变化: -{} | 订单: {}",
                event.getProductName(),
                event.getQuantity(),
                event.getOrderNo());
    }

    /**
     * 记录操作日志（模拟）
     */
    public void recordLog(OrderEvent event) {
        log.info("📝 [日志服务] 订单: {} | 用户: {} | 状态: {} | 时间: {}",
                event.getOrderNo(),
                event.getUsername(),
                event.getStatus(),
                event.getEventTime());
    }

    /**
     * 处理工作任务（模拟耗时操作）
     */
    public void processWorkTask(OrderEvent event) throws InterruptedException {
        log.info("⚙️  [工作队列] 开始处理订单: {} | 线程: {}",
                event.getOrderNo(), Thread.currentThread().getName());
        // 模拟耗时处理（500ms）
        Thread.sleep(500);
        log.info("✅ [工作队列] 处理完成: {}", event.getOrderNo());
    }

    /**
     * 处理死信消息
     */
    public void handleDeadLetter(OrderEvent event) {
        log.warn("💀 [死信队列] 收到死信消息 | 订单: {} | 原因: 消息处理失败或TTL过期 | 事件ID: {}",
                event.getOrderNo(), event.getEventId());
        // 实际应用中可以：告警、人工介入、持久化等
    }
}
