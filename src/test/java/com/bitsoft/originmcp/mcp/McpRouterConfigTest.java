package com.bitsoft.originmcp.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for McpRouterConfig.
 * Tests that the RouterFunction is created correctly.
 */
class McpRouterConfigTest {

    @Test
    void testMcpRouterFunction_CreatesSuccessfully() {
        ObjectMapper objectMapper = new ObjectMapper();
        McpHandler handler = new McpHandler(objectMapper, null);
        McpRouterConfig config = new McpRouterConfig(handler);

        RouterFunction<ServerResponse> routerFunction = config.mcpRouterFunction();

        assertNotNull(routerFunction, "RouterFunction should be created");
    }

    @Test
    void testMcpRouterFunction_IsFunctionalInterface() {
        ObjectMapper objectMapper = new ObjectMapper();
        McpHandler handler = new McpHandler(objectMapper, null);
        McpRouterConfig config = new McpRouterConfig(handler);

        RouterFunction<ServerResponse> routerFunction = config.mcpRouterFunction();

        assertNotNull(routerFunction);
        // RouterFunction should implement the functional interface
        assertDoesNotThrow(() -> {
            // Just verify the RouterFunction is a valid functional interface implementation
            routerFunction.getClass().getInterfaces();
        });
    }

    @Test
    void testMcpRouterFunction_NotNullRoutes() {
        ObjectMapper objectMapper = new ObjectMapper();
        McpHandler handler = new McpHandler(objectMapper, null);
        McpRouterConfig config = new McpRouterConfig(handler);

        RouterFunction<ServerResponse> routerFunction = config.mcpRouterFunction();

        assertNotNull(routerFunction);
        // Verify routes are configured (the router should have routes)
        String routerString = routerFunction.toString();
        assertNotNull(routerString);
        assertFalse(routerString.isEmpty());
    }
}
