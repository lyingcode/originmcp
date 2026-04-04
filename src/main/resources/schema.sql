-- MCP Tool Definition Table
-- Stores metadata for dynamically loaded MCP tools

CREATE TABLE IF NOT EXISTS mcp_tool_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tool_name VARCHAR(255) NOT NULL UNIQUE COMMENT 'Unique tool name (used as identifier)',
    description TEXT COMMENT 'Tool description for AI to understand when to use',
    service_bean_name VARCHAR(255) NOT NULL COMMENT 'Spring bean name of the service containing the method',
    method_name VARCHAR(255) NOT NULL COMMENT 'Name of the method to invoke',
    return_type VARCHAR(255) DEFAULT 'java.lang.String' COMMENT 'Return type class name',
    enabled BOOLEAN DEFAULT TRUE COMMENT 'Whether the tool is active',
    priority INT DEFAULT 0 COMMENT 'Higher priority tools are registered first',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_enabled (enabled),
    INDEX idx_service_bean (service_bean_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP tool definitions stored in database for dynamic loading';

-- MCP Tool Parameter Table
-- Stores parameter definitions for dynamically loaded tools

CREATE TABLE IF NOT EXISTS mcp_tool_parameter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tool_id BIGINT NOT NULL COMMENT 'FK to mcp_tool_definition.id',
    parameter_name VARCHAR(255) NOT NULL COMMENT 'Name of the parameter',
    parameter_type VARCHAR(255) NOT NULL DEFAULT 'java.lang.String' COMMENT 'Java type class name',
    parameter_order INT NOT NULL COMMENT 'Order of the parameter (0-indexed)',
    description VARCHAR(500) COMMENT 'Parameter description for AI',
    required BOOLEAN DEFAULT FALSE COMMENT 'Whether this parameter is required',
    default_value VARCHAR(500) COMMENT 'Default value if not required',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tool_parameter FOREIGN KEY (tool_id) REFERENCES mcp_tool_definition(id) ON DELETE CASCADE,
    UNIQUE KEY uk_tool_param_order (tool_id, parameter_order),
    INDEX idx_tool_id (tool_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Parameters for dynamically loaded MCP tools';

-- MCP API Keys Table
-- Stores API keys for client authentication

CREATE TABLE IF NOT EXISTS mcp_api_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL UNIQUE COMMENT 'Unique client identifier',
    api_key VARCHAR(255) NOT NULL UNIQUE COMMENT 'API key (stored as hash)',
    api_key_hash VARCHAR(255) NOT NULL COMMENT 'Salted SHA-256 hash of the API key',
    client_name VARCHAR(255) COMMENT 'Human-readable client name',
    enabled BOOLEAN DEFAULT TRUE COMMENT 'Whether this API key is active',
    rate_limit INT DEFAULT 60 COMMENT 'Requests per minute limit',
    rate_limit_enabled BOOLEAN DEFAULT TRUE COMMENT 'Whether rate limiting is enabled for this client',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL COMMENT 'Expiration time, NULL means never expires',
    INDEX idx_api_key_hash (api_key_hash),
    INDEX idx_client_id (client_id),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API keys for MCP server authentication';

-- MCP Client Permissions Table
-- Stores tool permissions for each client

CREATE TABLE IF NOT EXISTS mcp_client_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL COMMENT 'Client identifier (FK to mcp_api_keys.client_id)',
    tool_name VARCHAR(255) NOT NULL COMMENT 'Tool name the client is allowed to access',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_client_permissions FOREIGN KEY (client_id) REFERENCES mcp_api_keys(client_id) ON DELETE CASCADE,
    UNIQUE KEY uk_client_tool (client_id, tool_name),
    INDEX idx_client_id (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tool permissions for MCP clients';
