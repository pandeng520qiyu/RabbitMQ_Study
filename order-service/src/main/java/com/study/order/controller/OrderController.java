package com.study.order.controller;

import com.study.common.dto.ApiResponse;
import com.study.order.entity.Order;
import com.study.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单 REST API
 * <p>
 * 基础路径：/orders
 * 通过网关访问：http://localhost:8080/api/orders/...
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单（同时发送多种 RabbitMQ 消息）
     * POST /orders
     * <p>
     * 请求体示例：
     * {
     *   "userId": 1,
     *   "username": "张三",
     *   "productName": "RabbitMQ实战书籍",
     *   "quantity": 2,
     *   "amount": 99.00,
     *   "urgent": false,
     *   "remark": "尽快发货"
     * }
     */
    @PostMapping
    public ApiResponse<Order> createOrder(@Valid @RequestBody Order order) {
        Order created = orderService.createOrder(order);
        return ApiResponse.success("订单创建成功，已发送 RabbitMQ 消息", created);
    }

    /**
     * 创建订单（演示 TTL + 死信队列）
     * POST /orders/ttl
     * 消息 5 秒后过期，进入死信队列
     */
    @PostMapping("/ttl")
    public ApiResponse<Order> createOrderWithTtl(@Valid @RequestBody Order order) {
        Order created = orderService.createOrderWithTtl(order);
        return ApiResponse.success("TTL 订单创建成功（消息5秒后过期进入死信队列）", created);
    }

    /**
     * 支付订单
     * PUT /orders/{id}/pay
     */
    @PutMapping("/{id}/pay")
    public ApiResponse<Order> payOrder(@PathVariable Long id) {
        Order order = orderService.payOrder(id);
        return ApiResponse.success("支付成功，已发送支付通知消息", order);
    }

    /**
     * 取消订单
     * PUT /orders/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    public ApiResponse<Order> cancelOrder(@PathVariable Long id) {
        Order order = orderService.cancelOrder(id);
        return ApiResponse.success("订单已取消，已发送取消通知消息", order);
    }

    /**
     * 查询单个订单
     * GET /orders/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<Order> getOrder(@PathVariable Long id) {
        Order order = orderService.getOrderOrThrow(id);
        return ApiResponse.success(order);
    }

    /**
     * 查询所有订单
     * GET /orders
     */
    @GetMapping
    public ApiResponse<List<Order>> listOrders() {
        return ApiResponse.success(orderService.listOrders());
    }
}
