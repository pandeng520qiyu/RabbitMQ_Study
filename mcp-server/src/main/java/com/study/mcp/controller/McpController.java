package com.study.mcp.controller;

import com.study.common.dto.ApiResponse;
import com.study.mcp.model.McpModel;
import com.study.mcp.service.McpToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * MCP Server REST 控制器
 * <p>
 * 实现 Model Context Protocol (MCP) 规范的 HTTP 接口
 * <p>
 * MCP 协议简介：
 * - 由 Anthropic 于 2024 年发布的开放协议
 * - 目标：统一 AI 模型与外部工具/数据源的集成方式
 * - 传输层支持：HTTP + SSE（本实现）、stdio（命令行工具）
 * - 官方文档：https://modelcontextprotocol.io
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpController {

    private final McpToolService toolService;

    /**
     * MCP Server 信息
     * GET /mcp/info
     * <p>
     * AI 客户端首先调用此接口了解服务能力
     */
    @GetMapping("/info")
    public ApiResponse<McpModel.ServerInfo> getServerInfo() {
        McpModel.ServerInfo info = McpModel.ServerInfo.builder()
                .name("rabbitmq-study-mcp-server")
                .version("1.0.0")
                .protocolVersion("2024-11-05")
                .description("RabbitMQ 学习项目 MCP 服务器，提供订单管理和 RabbitMQ 查询工具")
                .capabilities(McpModel.ServerCapabilities.builder()
                        .tools(true)
                        .resources(true)
                        .prompts(false)
                        .logging(true)
                        .build())
                .build();
        return ApiResponse.success(info);
    }

    /**
     * 获取工具列表
     * GET /mcp/tools
     * <p>
     * AI 模型调用此接口获取所有可用工具的定义
     */
    @GetMapping("/tools")
    public ApiResponse<McpModel.ToolList> listTools() {
        return ApiResponse.success(McpModel.ToolList.builder()
                .tools(toolService.getTools())
                .build());
    }

    /**
     * 调用工具
     * POST /mcp/tools/call
     * <p>
     * AI 模型选择工具并传入参数，服务器执行并返回结果
     * <p>
     * 请求体示例：
     * {
     *   "name": "list_orders",
     *   "arguments": { "status": "PENDING" }
     * }
     */
    @PostMapping("/tools/call")
    public ApiResponse<McpModel.ToolCallResponse> callTool(@RequestBody McpModel.ToolCallRequest request) {
        log.info("[MCP] 收到工具调用请求: {} | 参数: {}", request.getName(), request.getArguments());
        McpModel.ToolCallResponse response = toolService.callTool(request.getName(), request.getArguments());
        return ApiResponse.success(response);
    }

    /**
     * 获取资源列表
     * GET /mcp/resources
     */
    @GetMapping("/resources")
    public ApiResponse<McpModel.ResourceList> listResources() {
        return ApiResponse.success(McpModel.ResourceList.builder()
                .resources(toolService.getResources())
                .build());
    }

    /**
     * JSON-RPC 2.0 端点（MCP 标准传输格式）
     * POST /mcp/rpc
     * <p>
     * MCP 协议底层使用 JSON-RPC 2.0，此端点处理标准 MCP 请求
     * <p>
     * 支持的方法：
     * - initialize          初始化连接
     * - tools/list          获取工具列表
     * - tools/call          调用工具
     * - resources/list      获取资源列表
     */
    @PostMapping("/rpc")
    public McpModel.JsonRpcResponse handleJsonRpc(@RequestBody McpModel.JsonRpcRequest request) {
        log.info("[MCP-RPC] method: {} | id: {}", request.getMethod(), request.getId());

        try {
            Object result = switch (request.getMethod()) {
                case "initialize" -> Map.of(
                        "protocolVersion", "2024-11-05",
                        "serverInfo", Map.of(
                                "name", "rabbitmq-study-mcp-server",
                                "version", "1.0.0"
                        ),
                        "capabilities", Map.of(
                                "tools", Map.of(),
                                "resources", Map.of()
                        )
                );
                case "tools/list" -> Map.of("tools", toolService.getTools());
                case "tools/call" -> {
                    Map<String, Object> params = request.getParams();
                    String toolName = (String) params.get("name");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Map.of());
                    yield toolService.callTool(toolName, arguments);
                }
                case "resources/list" -> Map.of("resources", toolService.getResources());
                case "notifications/initialized" -> null;  // 忽略通知
                default -> throw new IllegalArgumentException("不支持的方法: " + request.getMethod());
            };

            return McpModel.JsonRpcResponse.builder()
                    .jsonrpc("2.0")
                    .id(request.getId())
                    .result(result)
                    .build();

        } catch (Exception e) {
            log.error("[MCP-RPC] 处理失败: {}", e.getMessage());
            return McpModel.JsonRpcResponse.builder()
                    .jsonrpc("2.0")
                    .id(request.getId())
                    .error(McpModel.JsonRpcError.builder()
                            .code(-32601)
                            .message(e.getMessage())
                            .build())
                    .build();
        }
    }

    /**
     * SSE（Server-Sent Events）端点
     * GET /mcp/sse
     * <p>
     * MCP 支持 SSE 作为传输层，允许服务器主动推送事件给 AI 客户端
     * 这对于长时间运行的工具调用非常有用
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        SseEmitter emitter = new SseEmitter(30000L);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 发送连接建立事件
                emitter.send(SseEmitter.event()
                        .name("connected")
                        .data("{\"message\":\"MCP SSE 连接建立成功\",\"serverName\":\"rabbitmq-study-mcp-server\"}"));

                // 发送服务器能力信息
                emitter.send(SseEmitter.event()
                        .name("capabilities")
                        .data("{\"tools\":true,\"resources\":true}"));

                log.info("[MCP-SSE] 客户端已连接");
                // 实际应用中保持连接并推送事件
                // 这里演示完就关闭
                emitter.complete();
            } catch (IOException e) {
                log.warn("[MCP-SSE] 连接断开: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 学习指南：MCP 协议说明
     * GET /mcp/about
     */
    @GetMapping("/about")
    public ApiResponse<Map<String, String>> about() {
        Map<String, String> info = new java.util.LinkedHashMap<>();
        info.put("protocol", "Model Context Protocol (MCP)");
        info.put("version", "2024-11-05");
        info.put("official_docs", "https://modelcontextprotocol.io");
        info.put("description", "MCP 是 Anthropic 推出的开放协议，用于 AI 模型与外部工具的标准化集成");
        info.put("transport", "本实现使用 HTTP + SSE 传输层");
        info.put("api_info", "GET /mcp/info - 服务器信息");
        info.put("api_tools", "GET /mcp/tools - 工具列表");
        info.put("api_call", "POST /mcp/tools/call - 调用工具");
        info.put("api_resources", "GET /mcp/resources - 资源列表");
        info.put("api_rpc", "POST /mcp/rpc - JSON-RPC 2.0 端点（标准 MCP 传输格式）");
        info.put("api_sse", "GET /mcp/sse - SSE 流式事件");
        info.put("integration_hint", "可将此服务的 /mcp/rpc 端点配置到 Claude Desktop 或其他 MCP 客户端中");
        return ApiResponse.success("MCP Server 说明", info);
    }
}
