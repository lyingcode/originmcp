-- Calculator Service Tools Registration
-- This script registers CalculatorService methods as MCP tools

-- Clean up existing data (including weatherService tools that were removed)
DELETE FROM mcp_tool_parameter WHERE tool_id IN (
    SELECT id FROM mcp_tool_definition WHERE service_bean_name = 'calculatorService'
    OR service_bean_name = 'weatherService'
);
DELETE FROM mcp_tool_definition WHERE service_bean_name = 'calculatorService'
   OR service_bean_name = 'weatherService';

-- Insert tool definitions
INSERT INTO mcp_tool_definition (tool_name, description, service_bean_name, method_name, return_type, enabled, priority)
VALUES
    ('add', 'Add two numbers and return the sum', 'calculatorService', 'add', 'java.lang.Integer', TRUE, 100),
    ('subtract', 'Subtract second number from first number', 'calculatorService', 'subtract', 'java.lang.Integer', TRUE, 100),
    ('multiply', 'Multiply two numbers', 'calculatorService', 'multiply', 'java.lang.Integer', TRUE, 100),
    ('divide', 'Divide first number by second number', 'calculatorService', 'divide', 'java.lang.Double', TRUE, 100),
    ('greet', 'Get a greeting message', 'calculatorService', 'greet', 'java.lang.String', TRUE, 100),
    ('echo', 'Echo back the input text', 'calculatorService', 'echo', 'java.lang.String', TRUE, 100),
    ('currentTime', 'Get current timestamp in milliseconds', 'calculatorService', 'currentTimeMillis', 'java.lang.Long', TRUE, 100);

-- Get the base ID for calculator tools (assuming they are inserted first)
SET @base_id = LAST_INSERT_ID();

-- Insert parameters for 'add' tool
INSERT INTO mcp_tool_parameter (tool_id, parameter_name, parameter_type, parameter_order, description, required, default_value)
VALUES
    (@base_id, 'a', 'java.lang.Integer', 0, 'First number', TRUE, NULL),
    (@base_id + 1, 'a', 'java.lang.Integer', 0, 'First number', TRUE, NULL),
    (@base_id + 2, 'a', 'java.lang.Integer', 0, 'First number', TRUE, NULL),
    (@base_id + 3, 'a', 'java.lang.Integer', 0, 'Dividend', TRUE, NULL),
    (@base_id + 4, 'name', 'java.lang.String', 0, 'Name to greet', FALSE, 'World'),
    (@base_id + 5, 'text', 'java.lang.String', 0, 'Text to echo', TRUE, NULL);

-- Fix: Use correct tool IDs
DELETE FROM mcp_tool_parameter;
INSERT INTO mcp_tool_parameter (tool_id, parameter_name, parameter_type, parameter_order, description, required, default_value)
SELECT id, 'a', 'java.lang.Integer', 0, 'First number', TRUE, NULL FROM mcp_tool_definition WHERE method_name = 'add'
UNION ALL
SELECT id, 'b', 'java.lang.Integer', 1, 'Second number', TRUE, NULL FROM mcp_tool_definition WHERE method_name = 'add'
UNION ALL
SELECT id, 'a', 'java.lang.Integer', 0, 'First number', TRUE, NULL FROM mcp_tool_definition WHERE method_name = 'subtract'
UNION ALL
SELECT id, 'b', 'java.lang.Integer', 1, 'Second number', TRUE, NULL FROM mcp_tool_definition WHERE method_name = 'subtract'
UNION ALL
SELECT id, 'a', 'java.lang.Integer', 0, 'First number', TRUE, NULL FROM mcp_tool_definition WHERE method_name = 'multiply'
UNION ALL
SELECT id, 'b', 'java.lang.Integer', 1, 'Second number', TRUE, NULL FROM mcp_tool_definition WHERE method_name = 'multiply'
UNION ALL
SELECT id, 'a', 'java.lang.Integer', 0, 'Dividend', TRUE, NULL FROM mcp_tool_definition WHERE method_name = 'divide'
UNION ALL
SELECT id, 'b', 'java.lang.Integer', 1, 'Divisor', TRUE, NULL FROM mcp_tool_definition WHERE method_name = 'divide'
UNION ALL
SELECT id, 'name', 'java.lang.String', 0, 'Name to greet', FALSE, 'World' FROM mcp_tool_definition WHERE method_name = 'greet'
UNION ALL
SELECT id, 'text', 'java.lang.String', 0, 'Text to echo', TRUE, NULL FROM mcp_tool_definition WHERE method_name = 'echo';

-- API Key for testing (if mcp_api_keys table exists)
-- API key hash is SHA-256 of "test-api-key" with prefix "mcp-salt:"
INSERT IGNORE INTO mcp_api_keys (client_id, api_key, api_key_hash, client_name, enabled, rate_limit, rate_limit_enabled)
VALUES ('test-client', 'test-api-key', 'fRQVUnxwmUaLULgUm7AOnzxdko0klhe+LHW3DwnkFG0=', 'Test Client', TRUE, 60, TRUE);

-- Give test-client permission to use calculator tools
INSERT IGNORE INTO mcp_client_permissions (client_id, tool_name)
SELECT 'test-client', tool_name FROM mcp_tool_definition WHERE service_bean_name = 'calculatorService';
