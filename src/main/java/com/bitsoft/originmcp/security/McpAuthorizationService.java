package com.bitsoft.originmcp.security;

import com.bitsoft.originmcp.mapper.McpClientPermissionMapper;
import com.bitsoft.originmcp.model.database.McpApiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MCP Authorization service that checks if a client has permission to invoke a tool.
 * Implements default-deny policy: if no explicit permission is granted, access is denied.
 */
@Service
public class McpAuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(McpAuthorizationService.class);

    @Autowired
    private McpClientPermissionMapper permissionMapper;

    @Autowired
    private McpAuthenticator authenticator;

    // Cache: clientId -> set of allowed tool names
    private final java.util.concurrent.ConcurrentHashMap<String, Set<String>> permissionCache =
        new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Check if the current authenticated client has permission to invoke the specified tool.
     * Supports both STDIO (single client) and HTTP (per-request) authentication contexts.
     *
     * @param toolName The name of the tool to check
     * @return true if permission is granted, false otherwise
     */
    public boolean hasPermission(String toolName) {
        if (!authenticator.isSecurityEnabled()) {
            return true;
        }

        // Check HTTP auth context first (for HTTP transport)
        McpApiKey httpClient = HttpAuthContext.getCurrentClient();
        if (httpClient != null) {
            return hasPermission(httpClient.getClientId(), toolName);
        }

        // Fall back to STDIO context (single client)
        return authenticator.getCurrentClient()
            .map(client -> hasPermission(client.getClientId(), toolName))
            .orElse(false);
    }

    /**
     * Check if a specific client has permission to invoke a tool.
     *
     * @param clientId The client ID
     * @param toolName The tool name
     * @return true if permission is granted
     */
    public boolean hasPermission(String clientId, String toolName) {
        Set<String> allowedTools = getAllowedTools(clientId);
        boolean permitted = allowedTools.contains(toolName);

        if (!permitted) {
            log.warn("Access denied: client '{}' attempted to invoke tool '{}'",
                maskClientId(clientId), toolName);
        }

        return permitted;
    }

    /**
     * Get the list of tools that a client is allowed to invoke.
     * Returns empty set if no permissions are configured (default deny).
     *
     * @param clientId The client ID
     * @return Set of allowed tool names (immutable)
     */
    public Set<String> getAllowedTools(String clientId) {
        Set<String> cached = permissionCache.get(clientId);
        if (cached != null) {
            return cached;
        }

        // Load from database
        List<String> tools = permissionMapper.findToolNamesByClientId(clientId);
        Set<String> toolSet = new HashSet<>(tools);

        // Cache it
        permissionCache.put(clientId, Collections.unmodifiableSet(toolSet));

        log.debug("Loaded {} permissions for client '{}'", toolSet.size(), maskClientId(clientId));
        return toolSet;
    }

    /**
     * Clear permission cache for a client (call when permissions are updated).
     */
    public void invalidateCache(String clientId) {
        permissionCache.remove(clientId);
        log.debug("Invalidated permission cache for client: {}", maskClientId(clientId));
    }

    /**
     * Clear all permission cache.
     */
    public void invalidateAllCache() {
        permissionCache.clear();
        log.debug("Invalidated all permission cache");
    }

    /**
     * Reload permissions from database for a specific client.
     */
    public void reloadPermissions(String clientId) {
        invalidateCache(clientId);
        getAllowedTools(clientId); // Pre-load
    }

    private String maskClientId(String clientId) {
        if (clientId == null || clientId.length() <= 2) {
            return "***";
        }
        return clientId.charAt(0) + "***" + clientId.charAt(clientId.length() - 1);
    }
}
