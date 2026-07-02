package com.study.mcp;

import com.study.mcp.model.McpModel;
import com.study.mcp.service.McpToolService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * McpToolService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("McpToolService 单元测试")
class McpToolServiceTest {

    @InjectMocks
    private McpToolService mcpToolService;

    @Test
    @DisplayName("获取工具列表 - 应返回所有工具")
    void getTools_shouldReturnAllTools() {
        List<McpModel.Tool> tools = mcpToolService.getTools();

        assertThat(tools).isNotEmpty();
        assertThat(tools).hasSizeGreaterThanOrEqualTo(5);

        List<String> toolNames = tools.stream().map(McpModel.Tool::getName).toList();
        assertThat(toolNames).contains("list_orders", "get_order", "create_order",
                "check_rabbitmq_status", "get_rabbitmq_guide");

        tools.forEach(tool -> {
            assertThat(tool.getName()).isNotBlank();
            assertThat(tool.getDescription()).isNotBlank();
            assertThat(tool.getInputSchema()).isNotNull();
        });
    }

    @Test
    @DisplayName("调用工具 list_orders - 应返回订单列表")
    void callTool_listOrders_shouldReturnOrders() {
        McpModel.ToolCallResponse response = mcpToolService.callTool("list_orders", null);

        assertThat(response).isNotNull();
        assertThat(response.getIsError()).isFalse();
        assertThat(response.getContent()).isNotEmpty();
        assertThat(response.getContent().get(0).getText()).contains("订单列表");
    }

    @Test
    @DisplayName("调用工具 get_order - 应返回指定订单")
    void callTool_getOrder_shouldReturnOrder() {
        Map<String, Object> args = Map.of("orderId", 1);
        McpModel.ToolCallResponse response = mcpToolService.callTool("get_order", args);

        assertThat(response.getIsError()).isFalse();
        assertThat(response.getContent().get(0).getText()).contains("订单详情");
    }

    @Test
    @DisplayName("调用工具 create_order - 应创建订单")
    void callTool_createOrder_shouldCreateOrder() {
        Map<String, Object> args = Map.of(
                "username", "测试用户",
                "productName", "MCP测试商品",
                "quantity", 1,
                "amount", 99.0
        );
        McpModel.ToolCallResponse response = mcpToolService.callTool("create_order", args);

        assertThat(response.getIsError()).isFalse();
        assertThat(response.getContent().get(0).getText()).contains("订单创建成功");
    }

    @Test
    @DisplayName("调用未知工具 - 应返回错误提示")
    void callTool_unknownTool_shouldReturnUnknownMessage() {
        McpModel.ToolCallResponse response = mcpToolService.callTool("unknown_tool", null);

        assertThat(response.getIsError()).isFalse();
        assertThat(response.getContent().get(0).getText()).contains("未知工具");
    }

    @Test
    @DisplayName("获取 RabbitMQ 指南 - 无主题")
    void callTool_getRabbitMQGuide_noTopic_shouldReturnOverview() {
        McpModel.ToolCallResponse response = mcpToolService.callTool("get_rabbitmq_guide", Map.of());

        assertThat(response.getIsError()).isFalse();
        assertThat(response.getContent().get(0).getText()).contains("RabbitMQ 核心概念");
    }

    @Test
    @DisplayName("获取 RabbitMQ 指南 - 查询 exchange 主题")
    void callTool_getRabbitMQGuide_exchangeTopic() {
        McpModel.ToolCallResponse response = mcpToolService.callTool(
                "get_rabbitmq_guide", Map.of("topic", "exchange"));

        assertThat(response.getIsError()).isFalse();
        assertThat(response.getContent().get(0).getText()).contains("Direct Exchange");
    }

    @Test
    @DisplayName("获取资源列表 - 应返回所有资源")
    void getResources_shouldReturnAllResources() {
        List<McpModel.Resource> resources = mcpToolService.getResources();

        assertThat(resources).isNotEmpty();
        resources.forEach(r -> {
            assertThat(r.getUri()).isNotBlank();
            assertThat(r.getName()).isNotBlank();
        });
    }
}
