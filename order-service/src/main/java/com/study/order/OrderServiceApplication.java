package com.study.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 订单服务启动类
 * <p>
 * 端口：8081
 * 功能：
 * - 创建/管理订单
 * - 通过 RabbitMQ 发送订单事件（生产者）
 * - 演示 Direct/Topic/Fanout/DLQ 等消息模式
 */
@EnableDiscoveryClient
@EnableScheduling
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
