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

-- Sample Data: Weather Tools

INSERT INTO mcp_tool_definition (tool_name, description, service_bean_name, method_name, enabled, priority)
VALUES ('getWeather', 'Get current weather and forecast for a specific location by city name or coordinates', 'weatherService', 'fetchWeatherData', TRUE, 10);

INSERT INTO mcp_tool_parameter (tool_id, parameter_name, parameter_type, parameter_order, description, required)
SELECT id, 'location', 'java.lang.String', 0, 'City name (e.g. beijing, shenzhen) or coordinates (e.g. 39.9042,116.4074)', TRUE
FROM mcp_tool_definition WHERE tool_name = 'getWeather';

INSERT INTO mcp_tool_definition (tool_name, description, service_bean_name, method_name, enabled, priority)
VALUES ('getForecast', 'Get detailed 7-day weather forecast for a location', 'weatherService', 'getForecast', TRUE, 9);

INSERT INTO mcp_tool_parameter (tool_id, parameter_name, parameter_type, parameter_order, description, required)
SELECT id, 'location', 'java.lang.String', 0, 'City name or coordinates', TRUE
FROM mcp_tool_definition WHERE tool_name = 'getForecast';
