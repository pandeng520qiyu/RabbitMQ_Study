package com.study.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单事件 - 在微服务间传递的消息体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    /** 事件ID（幂等性控制） */
    private String eventId;

    /** 订单ID */
    private Long orderId;

    /** 订单编号 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 商品名称 */
    private String productName;

    /** 商品数量 */
    private Integer quantity;

    /** 订单金额 */
    private BigDecimal amount;

    /** 订单状态 */
    private OrderStatus status;

    /** 事件类型 */
    private String eventType;

    /** 是否紧急（影响 Topic 路由） */
    private Boolean urgent;

    /** 备注 */
    private String remark;

    /** 事件发生时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventTime;

    /** 订单创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public enum OrderStatus {
        /** 待支付 */
        PENDING,
        /** 已支付 */
        PAID,
        /** 已发货 */
        SHIPPED,
        /** 已完成 */
        COMPLETED,
        /** 已取消 */
        CANCELLED,
        /** 退款中 */
        REFUNDING
    }
}
