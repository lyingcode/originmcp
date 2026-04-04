## ADDED Requirements

### Requirement: HTTP Endpoint for MCP
The system SHALL provide an HTTP endpoint at `POST /origin/mcp` that accepts JSON-RPC 2.0 requests.

#### Scenario: Valid JSON-RPC request
- **WHEN** a client sends a POST request to `/origin/mcp` with valid JSON-RPC 2.0 request body
- **THEN** the server SHALL process the request and return a JSON-RPC 2.0 response

#### Scenario: Invalid JSON-RPC request
- **WHEN** a client sends a POST request to `/origin/mcp` with invalid JSON or non-JSON-RPC format
- **THEN** the server SHALL return HTTP 400 Bad Request with error details

#### Scenario: Missing request body
- **WHEN** a client sends a POST request to `/origin/mcp` without a request body
- **THEN** the server SHALL return HTTP 400 Bad Request

### Requirement: JSON-RPC 2.0 Compliance
The system SHALL comply with JSON-RPC 2.0 specification for request and response formatting.

#### Scenario: Request with method and params
- **WHEN** a JSON-RPC request contains `"jsonrpc":"2.0"`, `"method":"toolName"`, and `"params"`
- **THEN** the system SHALL process the method call and return result in `"result"` field

#### Scenario: Request with id
- **WHEN** a JSON-RPC request contains an `"id"` field
- **THEN** the response SHALL contain the same `"id"` field

#### Scenario: Notification (no id)
- **WHEN** a JSON-RPC request does not contain an `"id"` field (notification)
- **THEN** the server SHALL process the request but not send a response
