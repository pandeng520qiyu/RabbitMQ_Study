package com.study.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP 协议相关数据模型
 * <p>
 * 参考：https://modelcontextprotocol.io/specification
 */
public class McpModel {

    // ===================================================================
    // MCP Server Info
    // ===================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ServerInfo {
        private String name;
        private String version;
        private String protocolVersion;
        private ServerCapabilities capabilities;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerCapabilities {
        private boolean tools;
        private boolean resources;
        private boolean prompts;
        private boolean logging;
    }

    // ===================================================================
    // MCP Tool Definition
    // ===================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Tool {
        /** 工具名称（唯一标识） */
        private String name;
        /** 工具描述（AI 模型根据此决定是否调用） */
        private String description;
        /** 输入参数 Schema（JSON Schema 格式） */
        private Map<String, Object> inputSchema;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolList {
        private List<Tool> tools;
    }

    // ===================================================================
    // MCP Tool Call Request/Response
    // ===================================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCallRequest {
        /** 要调用的工具名称 */
        private String name;
        /** 工具调用参数 */
        private Map<String, Object> arguments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ToolCallResponse {
        /** 调用结果内容 */
        private List<Content> content;
        /** 是否发生错误 */
        private Boolean isError;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        /** 内容类型：text / image / resource */
        private String type;
        /** 文本内容 */
        private String text;
    }

    // ===================================================================
    // MCP Resource
    // ===================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Resource {
        /** 资源 URI */
        private String uri;
        /** 资源名称 */
        private String name;
        /** 资源描述 */
        private String description;
        /** MIME 类型 */
        private String mimeType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceList {
        private List<Resource> resources;
    }

    // ===================================================================
    // JSON-RPC 2.0 (MCP 底层传输协议)
    // ===================================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JsonRpcRequest {
        private String jsonrpc = "2.0";
        private String id;
        private String method;
        private Map<String, Object> params;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JsonRpcResponse {
        @Builder.Default
        private String jsonrpc = "2.0";
        private String id;
        private Object result;
        private JsonRpcError error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JsonRpcError {
        private int code;
        private String message;
        private Object data;
    }
}
