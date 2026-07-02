package com.study.order;

import com.study.order.entity.Order;
import com.study.order.producer.OrderMessageProducer;
import com.study.order.repository.OrderRepository;
import com.study.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 单元测试")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMessageProducer messageProducer;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .id(1L)
                .orderNo("ORD20240101001")
                .userId(1L)
                .username("张三")
                .productName("RabbitMQ实战书籍")
                .quantity(2)
                .amount(new BigDecimal("99.00"))
                .status(Order.OrderStatus.PENDING)
                .urgent(false)
                .build();
    }

    @Test
    @DisplayName("创建订单 - 应保存订单并发送 RabbitMQ 消息")
    void createOrder_shouldSaveAndSendMessages() {
        // given
        Order input = Order.builder()
                .userId(1L)
                .username("张三")
                .productName("RabbitMQ实战书籍")
                .quantity(2)
                .amount(new BigDecimal("99.00"))
                .urgent(false)
                .build();
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // when
        Order result = orderService.createOrder(input);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderNo()).isEqualTo("ORD20240101001");

        // 验证 RabbitMQ 消息发送（4种模式）
        verify(messageProducer, times(1)).sendOrderEvent(any(), anyString());
        verify(messageProducer, times(1)).sendTopicMessage(any(), anyString());
        verify(messageProducer, times(1)).broadcastOrderEvent(any());
        verify(messageProducer, times(1)).sendToWorkQueue(any());
    }

    @Test
    @DisplayName("支付订单 - 应更新状态并发送支付通知")
    void payOrder_shouldUpdateStatusAndSendNotification() {
        // given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setStatus(Order.OrderStatus.PAID);
            return o;
        });

        // when
        Order result = orderService.payOrder(1L);

        // then
        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.PAID);
        verify(messageProducer, times(1)).sendOrderEvent(any(), eq("order.paid"));
    }

    @Test
    @DisplayName("取消订单 - 应更新状态并发送取消通知")
    void cancelOrder_shouldUpdateStatusAndSendNotification() {
        // given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setStatus(Order.OrderStatus.CANCELLED);
            return o;
        });

        // when
        Order result = orderService.cancelOrder(1L);

        // then
        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        verify(messageProducer, times(1)).sendOrderEvent(any(), eq("order.cancelled"));
    }

    @Test
    @DisplayName("查询不存在的订单 - 应抛出异常")
    void getOrder_notFound_shouldThrowException() {
        // given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> orderService.getOrderOrThrow(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("订单不存在");
    }

    @Test
    @DisplayName("创建 TTL 订单 - 应发送带过期时间的消息")
    void createOrderWithTtl_shouldSendTtlMessage() {
        // given
        Order input = Order.builder()
                .userId(1L)
                .username("王五")
                .productName("测试商品")
                .quantity(1)
                .amount(new BigDecimal("9.99"))
                .build();
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // when
        Order result = orderService.createOrderWithTtl(input);

        // then
        assertThat(result).isNotNull();
        // TTL 消息只发送一次（sendWithTtl）
        verify(messageProducer, times(1)).sendWithTtl(any(), eq(5000L));
        // TTL 订单不发送其他消息
        verify(messageProducer, never()).sendOrderEvent(any(), anyString());
    }
}
