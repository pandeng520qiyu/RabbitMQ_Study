package com.study.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * MCP Server 启动类
 * <p>
 * Model Context Protocol (MCP) 是由 Anthropic 提出的开放协议，
 * 用于 AI 模型与外部工具/数据源的标准化集成。
 * <p>
 * 本模块实现了一个轻量级 MCP Server，提供：
 * 1. /mcp/info     - 服务器信息（协议版本、能力描述）
 * 2. /mcp/tools    - 工具列表（供 AI 模型调用）
 * 3. /mcp/call     - 工具调用入口
 * 4. /mcp/resources - 资源列表（订单数据等）
 * 5. /mcp/sse      - SSE 流式通信（实验性）
 * <p>
 * 端口：8083
 * 通过网关访问：http://localhost:8080/mcp/...
 */
@EnableDiscoveryClient
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
