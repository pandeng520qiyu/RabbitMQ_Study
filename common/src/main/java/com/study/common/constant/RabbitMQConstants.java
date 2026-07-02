package com.study.common.constant;

/**
 * RabbitMQ 交换机、队列、路由键常量
 * <p>
 * 涵盖以下消息模式：
 * 1. Direct Exchange  - 直接路由
 * 2. Topic Exchange   - 主题路由
 * 3. Fanout Exchange  - 广播
 * 4. Dead Letter Queue - 死信队列
 * 5. Delayed Message  - 延迟消息（需 rabbitmq_delayed_message_exchange 插件）
 */
public final class RabbitMQConstants {

    private RabbitMQConstants() {}

    // ========== 交换机名称 ==========

    /** Direct 交换机：用于订单状态精确路由 */
    public static final String ORDER_DIRECT_EXCHANGE = "order.direct.exchange";

    /** Topic 交换机：用于灵活的模式匹配路由 */
    public static final String ORDER_TOPIC_EXCHANGE = "order.topic.exchange";

    /** Fanout 交换机：广播订单事件给所有绑定队列 */
    public static final String ORDER_FANOUT_EXCHANGE = "order.fanout.exchange";

    /** 死信交换机：处理消费失败或超时的消息 */
    public static final String ORDER_DLX_EXCHANGE = "order.dlx.exchange";

    /** 延迟消息交换机（需要 delayed_message_exchange 插件） */
    public static final String ORDER_DELAYED_EXCHANGE = "order.delayed.exchange";

    // ========== 队列名称 ==========

    /** 订单创建队列（Direct） */
    public static final String ORDER_CREATED_QUEUE = "order.created.queue";

    /** 订单取消队列（Direct） */
    public static final String ORDER_CANCELLED_QUEUE = "order.cancelled.queue";

    /** 订单支付队列（Direct） */
    public static final String ORDER_PAID_QUEUE = "order.paid.queue";

    /** 邮件通知队列（Topic，匹配 order.#） */
    public static final String ORDER_EMAIL_QUEUE = "order.email.queue";

    /** 短信通知队列（Topic，匹配 *.urgent.*） */
    public static final String ORDER_SMS_QUEUE = "order.sms.queue";

    /** 广播队列1（Fanout，用于库存服务） */
    public static final String ORDER_INVENTORY_QUEUE = "order.inventory.queue";

    /** 广播队列2（Fanout，用于日志服务） */
    public static final String ORDER_LOG_QUEUE = "order.log.queue";

    /** 死信队列：存放处理失败的消息 */
    public static final String ORDER_DEAD_LETTER_QUEUE = "order.dead.letter.queue";

    /** 延迟消息队列：用于超时未支付订单提醒 */
    public static final String ORDER_DELAYED_QUEUE = "order.delayed.queue";

    /** 工作队列：演示竞争消费者模式 */
    public static final String ORDER_WORK_QUEUE = "order.work.queue";

    // ========== 路由键 ==========

    /** 订单创建路由键 */
    public static final String ROUTING_KEY_ORDER_CREATED = "order.created";

    /** 订单取消路由键 */
    public static final String ROUTING_KEY_ORDER_CANCELLED = "order.cancelled";

    /** 订单支付路由键 */
    public static final String ROUTING_KEY_ORDER_PAID = "order.paid";

    /** 紧急订单路由键（用于 Topic 匹配） */
    public static final String ROUTING_KEY_ORDER_URGENT = "order.urgent.notify";

    /** 死信路由键 */
    public static final String ROUTING_KEY_DEAD_LETTER = "order.dead.letter";

    /** 延迟消息路由键 */
    public static final String ROUTING_KEY_DELAYED = "order.delayed";
}
