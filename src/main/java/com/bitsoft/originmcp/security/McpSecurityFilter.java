package com.bitsoft.originmcp.security;

import com.bitsoft.originmcp.mapper.McpApiKeyMapper;
import com.bitsoft.originmcp.model.database.McpApiKey;
import jakarta.annotation.PostConstruct;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP 安全过滤器 - 整合认证和限流。
 * 简化后的单一安全组件。
 */
@Component
@Order(1)
public class McpSecurityFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(McpSecurityFilter.class);

    // ==================== 依赖 ====================

    @Autowired
    private McpApiKeyMapper apiKeyMapper;

    @Value("${mcp.security.api-key-header:X-API-Key}")
    private String apiKeyHeader;

    @Value("${mcp.security.api-key-env:MCP_API_KEY}")
    private String apiKeyEnvName;

    @Value("${mcp.security.enabled:false}")
    private boolean securityEnabled;

    @Value("${mcp.http.endpoint:/origin/mcp}")
    private String mcpEndpoint;

    @Value("${mcp.security.rate-limit.default:60}")
    private int defaultLimit;

    @Value("${mcp.security.rate-limit.window-seconds:60}")
    private int windowSeconds;

    // ==================== 状态 ====================

    // API Key Hash -> McpApiKey 缓存
    private final Map<String, McpApiKey> apiKeyCache = new ConcurrentHashMap<>();

    // Client ID -> RateLimiter
    private final Map<String, SlidingWindowLimiter> limiters = new ConcurrentHashMap<>();

    // 当前请求的认证客户端（ThreadLocal）
    private static final ThreadLocal<McpApiKey> currentClient = new ThreadLocal<>();

    // ==================== 生命周期 ====================

    @PostConstruct
    public void init() {
        if (securityEnabled) {
            loadApiKeys();
        }
        log.info("McpSecurityFilter initialized: securityEnabled={}", securityEnabled);
    }

    private void loadApiKeys() {
        apiKeyMapper.findAllEnabled().forEach(apiKey -> {
            String hash = hashApiKey(apiKey.getApiKey());
            apiKeyCache.put(hash, apiKey);
            log.debug("Loaded API key for client: {}", maskClientId(apiKey.getClientId()));
        });
        log.info("Loaded {} API keys", apiKeyCache.size());
    }

    // ==================== Filter 实现 ====================

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String requestUri = request.getRequestURI();

        // 仅处理 MCP 端点
        if (!requestUri.equals(mcpEndpoint) && !requestUri.equals(mcpEndpoint + "/")) {
            chain.doFilter(request, response);
            return;
        }

        // OPTIONS 直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            addCorsHeaders(response);
            chain.doFilter(request, response);
            return;
        }

        try {
            // 1. 认证
            Optional<McpApiKey> clientOpt = authenticate(request);
            if (clientOpt.isEmpty()) {
                log.warn("Authentication failed for: {}", requestUri);
                sendError(response, 401, -32001, "Unauthorized");
                return;
            }

            McpApiKey client = clientOpt.get();
            currentClient.set(client);

            // 2. 限流（仅 POST 请求）
            if ("POST".equalsIgnoreCase(request.getMethod()) && securityEnabled) {
                int limit = client.getRateLimit() != null ? client.getRateLimit() : defaultLimit;
                boolean rateLimitEnabled = client.getRateLimitEnabled() != null ? client.getRateLimitEnabled() : true;

                if (rateLimitEnabled && !tryAcquire(client.getClientId(), limit)) {
                    log.warn("Rate limit exceeded for: {}", maskClientId(client.getClientId()));
                    sendError(response, 429, -32002, "Rate limit exceeded");
                    return;
                }

                response.setHeader("X-RateLimit-Remaining", String.valueOf(getRemaining(client.getClientId(), limit)));
                response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            }

            chain.doFilter(request, response);

        } finally {
            currentClient.remove();
        }
    }

    // ==================== 认证 ====================

    private Optional<McpApiKey> authenticate(HttpServletRequest request) {
        if (!securityEnabled) {
            return Optional.of(new McpApiKey());
        }

        String apiKey = request.getHeader(apiKeyHeader);
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        String hash = hashApiKey(apiKey);
        McpApiKey client = apiKeyCache.get(hash);

        if (client == null || !client.isValid()) {
            return Optional.empty();
        }

        return Optional.of(client);
    }

    private String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(("mcp-salt:" + apiKey).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // ==================== 限流 ====================

    private boolean tryAcquire(String clientId, int limit) {
        if (limit <= 0) return true;
        return limiters.computeIfAbsent(clientId, k -> new SlidingWindowLimiter())
            .tryAcquire(limit);
    }

    private int getRemaining(String clientId, int limit) {
        SlidingWindowLimiter limiter = limiters.get(clientId);
        return limiter != null ? limiter.getRemaining(limit) : limit;
    }

    /**
     * 滑动窗口限流器
     */
    private static class SlidingWindowLimiter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();
        private final long windowMillis;

        SlidingWindowLimiter() {
            this(60000); // 默认 60 秒
        }

        SlidingWindowLimiter(long windowMillis) {
            this.windowMillis = windowMillis;
        }

        synchronized boolean tryAcquire(int limit) {
            long now = System.currentTimeMillis();
            if (now - windowStart >= windowMillis) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= limit;
        }

        synchronized int getRemaining(int limit) {
            long now = System.currentTimeMillis();
            if (now - windowStart >= windowMillis) {
                return limit;
            }
            return Math.max(0, limit - count.get());
        }
    }

    // ==================== 响应工具 ====================

    private void sendError(HttpServletResponse response, int httpStatus, int errorCode, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> error = Map.of(
            "jsonrpc", "2.0",
            "error", Map.of("code", errorCode, "message", message),
            "id", "null"
        );

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private void addCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, X-API-Key");
    }

    private String maskClientId(String clientId) {
        if (clientId == null || clientId.length() <= 2) return "***";
        return clientId.charAt(0) + "***" + clientId.charAt(clientId.length() - 1);
    }

    // 需要 Jackson ObjectMapper（注入方式）
    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    // ==================== 静态访问器 ====================

    public static McpApiKey getCurrentClient() {
        return currentClient.get();
    }
}
