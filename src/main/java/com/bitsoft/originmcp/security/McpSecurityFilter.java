package com.bitsoft.originmcp.security;

import com.bitsoft.originmcp.model.database.McpApiKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP Security Filter for MCP HTTP endpoint.
 * Handles API Key authentication and rate limiting.
 */
@Component
@Order(1)
public class McpSecurityFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(McpSecurityFilter.class);

    @Autowired
    private McpAuthenticator authenticator;

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${mcp.security.api-key-header:X-API-Key}")
    private String apiKeyHeader;

    @Value("${mcp.http.endpoint:/origin/mcp}")
    private String mcpEndpoint;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String requestUri = request.getRequestURI();

        // Only apply to MCP HTTP endpoint
        if (!requestUri.equals(mcpEndpoint) && !requestUri.equals(mcpEndpoint + "/")) {
            chain.doFilter(request, response);
            return;
        }

        // Skip auth for OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            addCorsHeaders(response);
            chain.doFilter(request, response);
            return;
        }

        // Only apply to POST requests
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 1. Authentication
            String apiKey = request.getHeader(apiKeyHeader);
            Optional<McpApiKey> authClient = authenticator.authenticateAndGetClient(apiKey);
            if (authClient.isEmpty()) {
                log.warn("Authentication failed for request to {}", requestUri);
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, -32001, "Unauthorized");
                return;
            }

            // Set HTTP auth context for this request
            HttpAuthContext.setCurrentClient(authClient.get());

            // Get client info for rate limiting
            McpApiKey client = authClient.get();

            // 2. Rate limiting (only if security is enabled)
            if (authenticator.isSecurityEnabled()) {
                String clientId = client.getClientId();
                int limit = client.getRateLimit() != null ? client.getRateLimit() : 60;
                boolean rateLimitEnabled = client.getRateLimitEnabled() != null ? client.getRateLimitEnabled() : true;

                if (rateLimitEnabled && !rateLimiter.tryAcquire(clientId, limit)) {
                    log.warn("Rate limit exceeded for client: {}", maskClientId(clientId));
                    int remaining = rateLimiter.getRemainingQuota(clientId, limit);
                    response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
                    sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, -32002, "Rate limit exceeded");
                    return;
                }

                // Add rate limit headers
                int remaining = rateLimiter.getRemainingQuota(clientId, limit);
                response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
                response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            }

            // Authentication and rate limiting passed
            chain.doFilter(request, response);
        } finally {
            // Clear HTTP auth context at end of request
            HttpAuthContext.clear();
        }
    }

    /**
     * Send JSON error response.
     */
    private void sendErrorResponse(HttpServletResponse response, int httpStatus, int errorCode, String message)
            throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = Map.of(
            "jsonrpc", "2.0",
            "error", Map.of(
                "code", errorCode,
                "message", message
            ),
            "id", null
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Add CORS headers.
     */
    private void addCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, X-API-Key");
    }

    /**
     * Mask client ID for logging.
     */
    private String maskClientId(String clientId) {
        if (clientId == null || clientId.length() <= 2) {
            return "***";
        }
        return clientId.charAt(0) + "***" + clientId.charAt(clientId.length() - 1);
    }
}
