package com.bitsoft.originmcp.security;

import com.bitsoft.originmcp.mapper.McpApiKeyMapper;
import com.bitsoft.originmcp.model.database.McpApiKey;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Authentication handler for STDIO transport.
 * Reads API Key from environment variable or configuration.
 */
@Component
public class McpAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(McpAuthenticator.class);

    @Autowired
    private McpApiKeyMapper apiKeyMapper;

    @Value("${mcp.security.api-key-env:MCP_API_KEY}")
    private String apiKeyEnvName;

    @Value("${mcp.security.enabled:true}")
    private boolean securityEnabled;

    // Cache for API key hash -> client mapping
    private final Map<String, McpApiKey> apiKeyCache = new ConcurrentHashMap<>();

    // Current authenticated client (for STDIO, single client at a time)
    private volatile McpApiKey currentClient;

    @PostConstruct
    public void init() {
        if (securityEnabled) {
            loadApiKeysFromDatabase();
        }
    }

    /**
     * Load all enabled API keys from database into cache.
     */
    public void loadApiKeysFromDatabase() {
        apiKeyMapper.findAllEnabled().forEach(apiKey -> {
            String hash = hashApiKey(apiKey.getApiKey());
            apiKeyCache.put(hash, apiKey);
            log.info("Loaded API key for client: {}", maskClientId(apiKey.getClientId()));
        });
        log.info("Loaded {} API keys into authentication cache", apiKeyCache.size());
    }

    /**
     * Reload API keys from database (called when keys are updated).
     */
    public void reloadApiKeys() {
        apiKeyCache.clear();
        loadApiKeysFromDatabase();
    }

    /**
     * Authenticate a request using the provided API key.
     * For HTTP transport, use authenticateAndGetClient() instead.
     *
     * @param apiKey The API key to authenticate
     * @return true if authentication succeeds
     */
    public boolean authenticate(String apiKey) {
        return authenticateAndGetClient(apiKey).isPresent();
    }

    /**
     * Authenticate a request and return the authenticated client.
     * This method is thread-safe and should be used for HTTP transport.
     *
     * @param apiKey The API key to authenticate
     * @return Optional containing the authenticated client, or empty if authentication fails
     */
    public java.util.Optional<McpApiKey> authenticateAndGetClient(String apiKey) {
        if (!securityEnabled) {
            return java.util.Optional.of(new McpApiKey()); // Dummy client for disabled security
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Authentication failed: no API key provided");
            return java.util.Optional.empty();
        }

        String hash = hashApiKey(apiKey);
        McpApiKey client = apiKeyCache.get(hash);

        if (client == null) {
            log.warn("Authentication failed: invalid API key");
            return java.util.Optional.empty();
        }

        if (!client.isValid()) {
            log.warn("Authentication failed: API key expired or disabled for client: {}",
                maskClientId(client.getClientId()));
            return java.util.Optional.empty();
        }

        currentClient = client;
        log.debug("Authenticated client: {}", maskClientId(client.getClientId()));
        return java.util.Optional.of(client);
    }

    /**
     * Get the currently authenticated client.
     */
    public Optional<McpApiKey> getCurrentClient() {
        return Optional.ofNullable(currentClient);
    }

    /**
     * Check if security is enabled.
     */
    public boolean isSecurityEnabled() {
        return securityEnabled;
    }

    /**
     * Hash API key using SHA-256.
     */
    private String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(("mcp-salt:" + apiKey).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
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
