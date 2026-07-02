# RabbitMQ_Study - SpringCloud 学习项目

> 当前仓库只有后端服务，没有独立前端页面；服务发现默认使用 Nacos，订单服务默认连接达梦数据库。

> 基于 **Spring Cloud + RabbitMQ** 的微服务学习项目，涵盖 RabbitMQ 核心消息模式，并支持后续学习 **MCP（Model Context Protocol）**。

---

## 📦 项目结构

```
rabbitmq-study/
├── common/              # 公共模块（DTO、事件、常量）
├── eureka-server/       # 历史 Eureka 示例模块（当前默认不启用）
├── gateway/             # API 网关（端口 8080）
├── order-service/       # 订单服务 - RabbitMQ 生产者（端口 8081）
├── notification-service/# 通知服务 - RabbitMQ 消费者（端口 8082）
├── mcp-server/          # MCP 服务器（端口 8083）
└── docker-compose.yml   # Docker 一键启动
```

---

## 🚀 快速启动

### 方式一：Docker Compose（推荐）

```bash
# 1. 构建各模块 jar
mvn clean package -DskipTests

# 2. 一键启动所有服务
docker-compose up -d

# 3. 查看服务状态
docker-compose ps
```

### 方式二：本地启动

**前提**：本地安装 RabbitMQ、Nacos（默认 localhost:8848）和达梦数据库（默认 localhost:5236）

```bash
# 按顺序启动：
# 1. 启动 Gateway
cd gateway && mvn spring-boot:run

# 2. 启动 Order Service
cd order-service && mvn spring-boot:run

# 3. 启动 Notification Service
cd notification-service && mvn spring-boot:run

# 4. 启动 MCP Server（可选）
cd mcp-server && mvn spring-boot:run
```

---

## 🔍 服务访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| Nacos 控制台 | http://localhost:8848/nacos | 服务注册列表 |
| API 网关 | http://localhost:8080 | 统一入口 |
| RabbitMQ 管理界面 | http://localhost:15672 | 账号: guest/guest |
| 订单服务 | http://localhost:8081 | 直接访问 |
| 通知服务 | http://localhost:8082 | 直接访问 |
| MCP 服务器 | http://localhost:8083 | MCP 学习 |
| 达梦数据库 | jdbc:dm://localhost:5236/RABBITMQ_STUDY | 订单数据存储 |

---

## 📚 RabbitMQ 学习指南

### 消息模式一览

| 模式 | 交换机类型 | 路由方式 | 使用场景 |
|------|------------|----------|----------|
| Simple | 默认（Direct） | 精确队列名 | 一对一消费 |
| Work Queue | 默认（Direct） | 精确队列名 | 多消费者负载均衡 |
| Fanout | Fanout | 忽略路由键 | 广播事件 |
| Direct | Direct | 精确路由键 | 精确通知 |
| Topic | Topic | 通配符路由键 | 灵活过滤 |

### Topic 通配符说明

```
*  匹配一个单词
#  匹配零个或多个单词

示例：
  "order.#"      → 匹配 order.created, order.paid.confirm ...
  "*.urgent.*"   → 匹配 order.urgent.notify, stock.urgent.alert ...
```

### 本项目 Exchange & Queue 拓扑

```
                    ┌─ order.created.queue   ←── (order.created)
order.direct.exchange ─┤─ order.paid.queue      ←── (order.paid)
                    └─ order.cancelled.queue ←── (order.cancelled)

                    ┌─ order.email.queue     ←── (order.#)
order.topic.exchange ──┤
                    └─ order.sms.queue       ←── (*.urgent.*)

                    ┌─ order.inventory.queue
order.fanout.exchange ─┤
                    └─ order.log.queue

order.dlx.exchange ──── order.dead.letter.queue  (死信队列)
```

---

## 🛠️ API 测试

### 创建订单（触发多种消息模式）

```bash
# 普通订单（Direct + Topic + Fanout + WorkQueue）
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "username": "张三",
    "productName": "RabbitMQ实战书籍",
    "quantity": 2,
    "amount": 99.00,
    "urgent": false,
    "remark": "学习用"
  }'

# 紧急订单（短信 + 邮件都会收到）
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 2,
    "username": "李四",
    "productName": "Spring Cloud教程",
    "quantity": 1,
    "amount": 199.00,
    "urgent": true
  }'

# TTL 订单（5秒后消息进入死信队列）
curl -X POST http://localhost:8081/orders/ttl \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 3,
    "username": "王五",
    "productName": "测试商品",
    "quantity": 1,
    "amount": 9.99
  }'
```

### 支付/取消订单

```bash
# 支付订单（发送 Direct 消息）
curl -X PUT http://localhost:8081/orders/1/pay

# 取消订单（发送 Direct 消息）
curl -X PUT http://localhost:8081/orders/1/cancel
```

### 查询 RabbitMQ 学习指南

```bash
# 学习指南
curl http://localhost:8081/rabbit/guide

# 队列状态
curl http://localhost:8081/rabbit/queues
```

---

## 🤖 MCP 服务学习

MCP（Model Context Protocol）是 Anthropic 推出的开放协议，用于 AI 模型与外部工具的标准化集成。

### MCP API 示例

```bash
# 查看服务信息
curl http://localhost:8083/mcp/info

# 查看可用工具列表
curl http://localhost:8083/mcp/tools

# 调用工具 - 查询订单
curl -X POST http://localhost:8083/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{
    "name": "list_orders",
    "arguments": { "status": "PENDING" }
  }'

# 调用工具 - 创建订单
curl -X POST http://localhost:8083/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{
    "name": "create_order",
    "arguments": {
      "username": "AI用户",
      "productName": "MCP学习资料",
      "quantity": 1,
      "amount": 59.99
    }
  }'

# JSON-RPC 2.0 接口（标准 MCP 传输格式）
curl -X POST http://localhost:8083/mcp/rpc \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "tools/list",
    "params": {}
  }'

# SSE 流式连接
curl -N http://localhost:8083/mcp/sse
```

### 配置到 Claude Desktop

在 Claude Desktop 配置文件中添加（`~/Library/Application Support/Claude/claude_desktop_config.json`）：

```json
{
  "mcpServers": {
    "rabbitmq-study": {
      "command": "curl",
      "args": ["-X", "POST", "http://localhost:8083/mcp/rpc"]
    }
  }
}
```

---

## 🔑 核心知识点

### 1. 消息可靠性保障

```
生产者 → Publisher Confirm → 交换机
                              ↓ Publisher Returns（路由失败回调）
                            队列
                              ↓
消费者 ← 手动 ACK/NACK ← 消息处理
                              ↓（处理失败 + requeue=false）
                          死信队列
```

### 2. 幂等性处理

每条消息携带唯一 `eventId`，消费者检查是否已处理过该 ID，防止重复消费：

```java
if (!processedEventIds.add(eventId)) {
    // 重复消息，跳过
    channel.basicAck(deliveryTag, false);
    return;
}
```

### 3. 死信队列配置

```java
Map<String, Object> args = new HashMap<>();
args.put("x-dead-letter-exchange", "order.dlx.exchange");
args.put("x-dead-letter-routing-key", "order.dead.letter");
args.put("x-message-ttl", 30000);  // 30秒 TTL
Queue queue = QueueBuilder.durable("order.created.queue").withArguments(args).build();
```

### 4. Work Queue 公平分发

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        prefetch: 1        # 每次只预取1条，处理完再取下一条
        concurrency: 2     # 2个并发消费者
        acknowledge-mode: manual  # 手动 ACK
```

---

## 🛡️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.1.5 | 基础框架 |
| Spring Cloud | 2022.0.4 | 微服务组件 |
| Spring AMQP | 3.x | RabbitMQ 集成 |
| Nacos Discovery | - | 服务注册发现 |
| Spring Cloud Gateway | - | API 网关 |
| Dameng Database | - | 订单持久化 |
| RabbitMQ | 3.12 | 消息队列 |
| Java | 17 | 运行环境 |
| Docker | - | 容器化部署 |

---

## 📖 学习路径建议

1. **第一步**：启动 RabbitMQ，访问管理界面 http://localhost:15672，熟悉 Exchange/Queue/Binding 概念
2. **第二步**：调用 `POST /orders` 创建订单，观察 4 种消息模式同时触发
3. **第三步**：在 notification-service 的日志中观察各消费者的消费过程
4. **第四步**：调用 `POST /orders/ttl` 测试 TTL + 死信队列
5. **第五步**：访问 `GET /rabbit/guide` 查看学习指南
6. **第六步**：探索 MCP Server（`http://localhost:8083/mcp/about`），了解 AI 工具集成

---

*项目持续更新中，欢迎提 Issue 和 PR！*
