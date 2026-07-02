# 前端 · RabbitMQ 学习平台

纯 HTML / CSS / JavaScript 实现，无需任何构建工具，打开即用。

## 快速启动

```bash
# 方式一：VS Code Live Server（推荐）
# 安装扩展 "Live Server"，右键 index.html → Open with Live Server

# 方式二：Python 内置服务器
cd frontend
python3 -m http.server 5500
# 访问 http://localhost:5500
```

## 页面说明

| 页面 | 路径 | 说明 |
|------|------|------|
| 登录/注册 | `index.html` | 调用 `POST /api/auth/login` 获取 JWT |
| 概览 | `dashboard.html#overview` | 统计卡片 + 架构流程图 |
| 核心概念 | `dashboard.html#concepts` | 12 个 RabbitMQ 核心概念卡片 |
| 消息模式 | `dashboard.html#patterns` | 5 种消息模式图解 + 代码示例 |
| 订单管理 | `dashboard.html#orders` | 创建/支付/取消订单，实时触发 RabbitMQ 消息 |
| 快速演示 | `dashboard.html#demo` | 直接调用 API 并查看响应结果 |

## 演示账户

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | ADMIN |
| guest | guest123 | USER |

## 前置条件

启动后端服务（参考根目录 `docker-compose.yml`）：

```bash
docker-compose up -d nacos rabbitmq
# 等待 nacos/rabbitmq 就绪后启动业务服务
docker-compose up -d user-service gateway order-service notification-service
```

各服务端口：

- Gateway：http://localhost:8080
- User Service：http://localhost:8084
- Order Service：http://localhost:8081
- Notification Service：http://localhost:8082
- RabbitMQ 管理界面：http://localhost:15672（guest/guest）
- Nacos：http://localhost:8848/nacos
