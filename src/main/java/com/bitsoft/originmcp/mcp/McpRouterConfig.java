package com.bitsoft.originmcp.mcp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Router Function configuration for MCP HTTP endpoint.
 * Replaces traditional @RestController with functional routing.
 *
 * Route: POST /origin/mcp -> McpHandler.handleJsonRpc()
 *        OPTIONS /origin/mcp -> McpHandler.handleOptions()
 */
@Configuration
public class McpRouterConfig {

    private final McpHandler mcpHandler;

    public McpRouterConfig(McpHandler mcpHandler) {
        this.mcpHandler = mcpHandler;
    }

    /**
     * Define reactive routes for MCP endpoint.
     * Uses RouterFunctions (WebFlux) instead of @RequestMapping annotations.
     */
    @Bean
    public RouterFunction<ServerResponse> mcpRouterFunction() {
        return RouterFunctions.route()
            // POST /origin/mcp - handle JSON-RPC requests
            .POST("/origin/mcp",
                RequestPredicates.contentType(MediaType.APPLICATION_JSON),
                mcpHandler::handleJsonRpc)
            // OPTIONS /origin/mcp - handle CORS preflight
            .OPTIONS("/origin/mcp",
                mcpHandler::handleOptions)
            // Build the router function
            .build();
    }
}
