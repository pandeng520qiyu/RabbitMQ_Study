package com.study.mcp.service;

import com.study.mcp.model.McpModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * MCP 工具实现服务
 * <p>
 * 定义并实现供 AI 模型调用的工具
 */
@Slf4j
@Service
public class McpToolService {

    /** 模拟订单存储（实际应调用 order-service） */
    private final List<Map<String, Object>> mockOrders = new ArrayList<>();

    public McpToolService() {
        // 初始化一些模拟数据
        mockOrders.add(createMockOrder(1L, "ORD20240101001", "张三", "RabbitMQ实战书籍", 2, "99.00", "PENDING"));
        mockOrders.add(createMockOrder(2L, "ORD20240101002", "李四", "Spring Cloud微服务教程", 1, "199.00", "PAID"));
        mockOrders.add(createMockOrder(3L, "ORD20240101003", "王五", "分布式系统设计", 3, "149.00", "COMPLETED"));
    }

    /**
     * 获取所有可用工具列表
     */
    public List<McpModel.Tool> getTools() {
        List<McpModel.Tool> tools = new ArrayList<>();

        // 工具1：查询订单列表
        tools.add(McpModel.Tool.builder()
                .name("list_orders")
                .description("查询所有订单列表，返回订单ID、订单号、用户名、商品、金额、状态等信息")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "status", Map.of(
                                        "type", "string",
                                        "description", "按状态筛选，可选值: PENDING/PAID/SHIPPED/COMPLETED/CANCELLED"
                                )
                        ),
                        "required", List.of()
                ))
                .build());

        // 工具2：查询单个订单
        tools.add(McpModel.Tool.builder()
                .name("get_order")
                .description("根据订单ID查询订单详情")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "orderId", Map.of(
                                        "type", "integer",
                                        "description", "订单ID"
                                )
                        ),
                        "required", List.of("orderId")
                ))
                .build());

        // 工具3：创建订单
        tools.add(McpModel.Tool.builder()
                .name("create_order")
                .description("创建新订单，同时会触发 RabbitMQ 消息发送")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "username", Map.of("type", "string", "description", "用户名"),
                                "productName", Map.of("type", "string", "description", "商品名称"),
                                "quantity", Map.of("type", "integer", "description", "购买数量"),
                                "amount", Map.of("type", "number", "description", "订单金额"),
                                "urgent", Map.of("type", "boolean", "description", "是否紧急订单")
                        ),
                        "required", List.of("username", "productName", "quantity", "amount")
                ))
                .build());

        // 工具4：查询 RabbitMQ 状态
        tools.add(McpModel.Tool.builder()
                .name("check_rabbitmq_status")
                .description("查询 RabbitMQ 消息队列状态，包括各队列的消息数量")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(),
                        "required", List.of()
                ))
                .build());

        // 工具5：获取 RabbitMQ 学习指南
        tools.add(McpModel.Tool.builder()
                .name("get_rabbitmq_guide")
                .description("获取 RabbitMQ 核心概念学习指南，包括各种消息模式的说明")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "topic", Map.of(
                                        "type", "string",
                                        "description", "查询特定主题，如: exchange/queue/dlq/ack/idempotent"
                                )
                        ),
                        "required", List.of()
                ))
                .build());

        return tools;
    }

    /**
     * 执行工具调用
     */
    public McpModel.ToolCallResponse callTool(String toolName, Map<String, Object> arguments) {
        log.info("[MCP] 工具调用: {} | 参数: {}", toolName, arguments);

        try {
            String result = switch (toolName) {
                case "list_orders"          -> listOrders(arguments);
                case "get_order"            -> getOrder(arguments);
                case "create_order"         -> createOrder(arguments);
                case "check_rabbitmq_status" -> checkRabbitMQStatus();
                case "get_rabbitmq_guide"   -> getRabbitMQGuide(arguments);
                default -> "未知工具: " + toolName;
            };

            return McpModel.ToolCallResponse.builder()
                    .content(List.of(McpModel.Content.builder().type("text").text(result).build()))
                    .isError(false)
                    .build();

        } catch (Exception e) {
            log.error("[MCP] 工具调用失败: {}", e.getMessage());
            return McpModel.ToolCallResponse.builder()
                    .content(List.of(McpModel.Content.builder().type("text").text("错误: " + e.getMessage()).build()))
                    .isError(true)
                    .build();
        }
    }

    private String listOrders(Map<String, Object> args) {
        String statusFilter = args != null ? (String) args.get("status") : null;
        List<Map<String, Object>> result = statusFilter == null
                ? new ArrayList<>(mockOrders)
                : mockOrders.stream().filter(o -> statusFilter.equals(o.get("status"))).toList();

        StringBuilder sb = new StringBuilder("订单列表（共 " + result.size() + " 条）：\n");
        result.forEach(order -> sb.append(String.format(
                "- ID: %s | 订单号: %s | 用户: %s | 商品: %s | 金额: %s元 | 状态: %s\n",
                order.get("id"), order.get("orderNo"), order.get("username"),
                order.get("productName"), order.get("amount"), order.get("status")
        )));
        return sb.toString();
    }

    private String getOrder(Map<String, Object> args) {
        int orderId = ((Number) args.get("orderId")).intValue();
        return mockOrders.stream()
                .filter(o -> ((Number) o.get("id")).intValue() == orderId)
                .findFirst()
                .map(o -> String.format(
                        "订单详情：\n订单ID: %s\n订单号: %s\n用户名: %s\n商品: %s\n数量: %s\n金额: %s元\n状态: %s\n创建时间: %s",
                        o.get("id"), o.get("orderNo"), o.get("username"), o.get("productName"),
                        o.get("quantity"), o.get("amount"), o.get("status"), o.get("createTime")
                ))
                .orElse("未找到订单 ID: " + orderId);
    }

    private String createOrder(Map<String, Object> args) {
        long newId = mockOrders.size() + 1;
        String orderNo = "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + newId;
        Map<String, Object> order = createMockOrder(
                newId, orderNo,
                (String) args.getOrDefault("username", "游客"),
                (String) args.getOrDefault("productName", "未知商品"),
                ((Number) args.getOrDefault("quantity", 1)).intValue(),
                String.valueOf(args.getOrDefault("amount", "0")),
                "PENDING"
        );
        mockOrders.add(order);
        return String.format("✅ 订单创建成功！\n订单ID: %s\n订单号: %s\n状态: PENDING\n已触发 RabbitMQ 消息发送（Direct/Topic/Fanout 三种模式）",
                newId, orderNo);
    }

    private String checkRabbitMQStatus() {
        return """
                RabbitMQ 队列状态（模拟数据，实际需连接 RabbitMQ）：
                ┌─────────────────────────────┬──────────┬──────┐
                │ 队列名称                    │ 消息数量 │ 消费者 │
                ├─────────────────────────────┼──────────┼──────┤
                │ order.created.queue         │   3      │  1   │
                │ order.paid.queue            │   1      │  1   │
                │ order.cancelled.queue       │   0      │  1   │
                │ order.email.queue           │   2      │  1   │
                │ order.sms.queue             │   0      │  1   │
                │ order.inventory.queue       │   3      │  1   │
                │ order.log.queue             │   3      │  1   │
                │ order.dead.letter.queue     │   0      │  1   │
                │ order.work.queue            │   5      │  2   │
                └─────────────────────────────┴──────────┴──────┘
                
                访问 RabbitMQ 管理界面：http://localhost:15672 (guest/guest)
                """;
    }

    private String getRabbitMQGuide(Map<String, Object> args) {
        String topic = args != null ? (String) args.get("topic") : null;
        if (topic == null) {
            return """
                    RabbitMQ 核心概念：
                    
                    1. Exchange（交换机）：消息路由器
                       - Direct: 精确匹配路由键
                       - Topic:  通配符匹配（* 一个词，# 多个词）
                       - Fanout: 广播，忽略路由键
                       - Headers: 根据消息头匹配
                    
                    2. Queue（队列）：消息存储
                       - Durable: 持久化队列，重启后依然存在
                       - TTL: 消息/队列过期时间
                       - DLX: 死信交换机配置
                    
                    3. Binding（绑定）：交换机与队列的关联关系
                    
                    4. 消息确认（ACK）：
                       - basicAck: 确认处理成功
                       - basicNack: 拒绝消息（可选重新入队）
                    
                    5. 死信队列（DLQ）：处理失败消息的兜底机制
                    
                    查询详情：?topic=exchange|queue|dlq|ack|idempotent
                    """;
        }
        return switch (topic.toLowerCase()) {
            case "exchange" -> """
                    Exchange 类型详解：
                    
                    Direct Exchange（直连）：
                    - 路由键完全匹配才转发
                    - 适用：精确通知，如支付成功→支付队列
                    
                    Topic Exchange（主题）：
                    - * 匹配一个单词，# 匹配零个或多个单词
                    - 如：order.# 匹配 order.created, order.paid.confirm
                    - 如：*.urgent.* 匹配 order.urgent.notify
                    - 适用：灵活的消息过滤
                    
                    Fanout Exchange（扇出）：
                    - 忽略路由键，广播给所有绑定队列
                    - 适用：事件广播，如订单创建→库存/日志/统计同时更新
                    """;
            case "dlq", "dead letter" -> """
                    死信队列（Dead Letter Queue）：
                    
                    消息变为死信的三种情况：
                    1. 消费者调用 basicReject/basicNack 且 requeue=false
                    2. 消息 TTL 过期
                    3. 队列达到最大长度（x-max-length）
                    
                    配置方式（队列参数）：
                    x-dead-letter-exchange: order.dlx.exchange
                    x-dead-letter-routing-key: order.dead.letter
                    x-message-ttl: 30000  # 30秒后过期
                    
                    典型应用：
                    - 超时未支付订单处理
                    - 消费失败消息的兜底处理
                    - 消息延迟（TTL + DLQ 实现延迟队列）
                    """;
            case "ack" -> """
                    消息确认机制（ACK）：
                    
                    自动 ACK（不推荐生产使用）：
                    - 消息发送后立即确认，无论是否处理成功
                    
                    手动 ACK（推荐）：
                    - channel.basicAck(deliveryTag, false)   // 处理成功
                    - channel.basicNack(deliveryTag, false, false)  // 处理失败，不重入队→死信
                    - channel.basicNack(deliveryTag, false, true)   // 处理失败，重新入队
                    - channel.basicReject(deliveryTag, false)        // 拒绝单条消息
                    
                    prefetch 配置（公平分发）：
                    - prefetch=1：每个消费者最多持有1条未确认消息
                    - 防止某个消费者积压过多消息
                    """;
            case "idempotent" -> """
                    幂等性处理：
                    
                    为什么需要幂等性？
                    - 网络故障可能导致消息重复投递
                    - ACK 丢失后 Broker 会重新投递
                    
                    实现方案：
                    1. 数据库唯一约束（最简单）
                    2. Redis SET NX（推荐）：setIfAbsent(eventId, "1", 1, DAYS)
                    3. 状态机检查：处理前检查当前状态是否允许操作
                    
                    本项目实现：
                    - 每条消息携带 eventId（UUID）
                    - 消费者使用 ConcurrentHashMap 记录已处理 ID
                    - 生产环境应改为 Redis
                    """;
            default -> "未知主题: " + topic + "，可用主题: exchange/queue/dlq/ack/idempotent";
        };
    }

    /**
     * 获取资源列表
     */
    public List<McpModel.Resource> getResources() {
        return List.of(
                McpModel.Resource.builder()
                        .uri("orders://list")
                        .name("订单列表")
                        .description("当前系统中的所有订单数据")
                        .mimeType("application/json")
                        .build(),
                McpModel.Resource.builder()
                        .uri("rabbitmq://queues")
                        .name("RabbitMQ 队列状态")
                        .description("各消息队列的实时状态信息")
                        .mimeType("application/json")
                        .build(),
                McpModel.Resource.builder()
                        .uri("guide://rabbitmq")
                        .name("RabbitMQ 学习指南")
                        .description("RabbitMQ 核心概念和使用示例")
                        .mimeType("text/markdown")
                        .build()
        );
    }

    private Map<String, Object> createMockOrder(Long id, String orderNo, String username,
                                                 String productName, int quantity, String amount, String status) {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("id", id);
        order.put("orderNo", orderNo);
        order.put("username", username);
        order.put("productName", productName);
        order.put("quantity", quantity);
        order.put("amount", new BigDecimal(amount));
        order.put("status", status);
        order.put("createTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return order;
    }
}
