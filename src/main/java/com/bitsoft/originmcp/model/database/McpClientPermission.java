package com.bitsoft.originmcp.model.database;

import java.time.LocalDateTime;

/**
 * Entity representing a tool permission for a client.
 */
public class McpClientPermission {
    private Long id;
    private String clientId;
    private String toolName;
    private LocalDateTime createdAt;

    public McpClientPermission() {
    }

    public McpClientPermission(String clientId, String toolName) {
        this.clientId = clientId;
        this.toolName = toolName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
