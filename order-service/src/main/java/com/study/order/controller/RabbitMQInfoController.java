package com.study.order.controller;

import com.study.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RabbitMQ 学习辅助 Controller
 * 提供队列信息查询等功能，方便学习调试
 */
@RestController
@RequestMapping("/rabbit")
@RequiredArgsConstructor
public class RabbitMQInfoController {

    private final RabbitAdmin rabbitAdmin;

    /**
     * 查询各队列消息数量
     * GET /rabbit/queues
     */
    @GetMapping("/queues")
    public ApiResponse<Map<String, Object>> getQueueInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        String[] queues = {
            "order.created.queue",
            "order.cancelled.queue",
            "order.paid.queue",
            "order.email.queue",
            "order.sms.queue",
            "order.inventory.queue",
            "order.log.queue",
            "order.dead.letter.queue",
            "order.work.queue"
        };

        for (String queue : queues) {
            try {
                var props = rabbitAdmin.getQueueProperties(queue);
                if (props != null) {
                    info.put(queue, props);
                } else {
                    info.put(queue, "队列不存在或 RabbitMQ 未连接");
                }
            } catch (Exception e) {
                info.put(queue, "查询失败: " + e.getMessage());
            }
        }

        return ApiResponse.success("队列信息", info);
    }

    /**
     * RabbitMQ 学习指南
     * GET /rabbit/guide
     */
    @GetMapping("/guide")
    public ApiResponse<Map<String, String>> getLearningGuide() {
        Map<String, String> guide = new LinkedHashMap<>();
        guide.put("1_SimpleQueue",    "最简单模式：生产者→队列→消费者（一对一）");
        guide.put("2_WorkQueue",      "工作队列：一个队列，多个消费者竞争消费（负载均衡）");
        guide.put("3_Fanout",         "广播模式：一条消息发给所有绑定的队列（忽略路由键）");
        guide.put("4_Direct",         "直连模式：精确匹配路由键，路由到指定队列");
        guide.put("5_Topic",          "主题模式：路由键支持通配符（* 匹配一个词，# 匹配多个词）");
        guide.put("6_TTL",            "消息过期：消息或队列设置 TTL，超时进入死信队列");
        guide.put("7_DeadLetter",     "死信队列：处理被拒绝/超时/队列满的消息");
        guide.put("8_Confirm",        "消息确认：Publisher Confirm + Consumer ACK 保证可靠性");
        guide.put("9_Idempotent",     "幂等性：消费者需处理消息重复投递（通过 eventId 去重）");
        guide.put("API_ORDER_CREATE", "POST /orders  → 创建订单（同时演示4种消息模式）");
        guide.put("API_TTL_CREATE",   "POST /orders/ttl  → 演示 TTL + 死信队列");
        guide.put("API_PAY",          "PUT /orders/{id}/pay  → 支付（发送 Direct 消息）");
        guide.put("API_CANCEL",       "PUT /orders/{id}/cancel  → 取消（发送 Direct 消息）");
        guide.put("RABBITMQ_UI",      "http://localhost:15672  账号: guest/guest");
        return ApiResponse.success("RabbitMQ 学习指南", guide);
    }
}
