package com.study.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 通知服务启动类
 * <p>
 * 端口：8082
 * 功能：
 * - 消费 RabbitMQ 消息（消费者）
 * - 演示手动 ACK / NACK
 * - 演示死信队列消费
 * - 演示幂等性处理
 */
@EnableDiscoveryClient
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
