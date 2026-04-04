package com.bitsoft.originmcp.security;

import com.bitsoft.originmcp.model.database.McpApiKey;

/**
 * Request-scoped context for HTTP authentication.
 * Stores the currently authenticated client for the incoming HTTP request.
 */
public class HttpAuthContext {

    private static final ThreadLocal<McpApiKey> currentClient = new ThreadLocal<>();

    /**
     * Set the authenticated client for the current request.
     */
    public static void setCurrentClient(McpApiKey client) {
        currentClient.set(client);
    }

    /**
     * Get the authenticated client for the current request.
     */
    public static McpApiKey getCurrentClient() {
        return currentClient.get();
    }

    /**
     * Clear the context (at end of request).
     */
    public static void clear() {
        currentClient.remove();
    }
}
