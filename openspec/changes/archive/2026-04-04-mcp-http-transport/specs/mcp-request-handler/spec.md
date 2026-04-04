## ADDED Requirements

### Requirement: Request Routing
The system SHALL route JSON-RPC requests to the appropriate tool based on the `method` field.

#### Scenario: Valid tool invocation
- **WHEN** a request contains a valid method name that matches a registered tool
- **THEN** the system SHALL invoke the tool with provided parameters and return the result

#### Scenario: Unknown method
- **WHEN** a request contains a method name that does not match any registered tool
- **THEN** the system SHALL return JSON-RPC error with code -32601 (Method not found)

### Requirement: Tool Result Serialization
The system SHALL serialize tool invocation results to JSON for the JSON-RPC response.

#### Scenario: Successful tool execution
- **WHEN** a tool executes successfully and returns a result
- **THEN** the system SHALL return `{"jsonrpc":"2.0","result":<result>,"id":<id>}`

#### Scenario: Tool execution error
- **WHEN** a tool throws an exception during execution
- **THEN** the system SHALL return `{"jsonrpc":"2.0","error":{"code":-32000,"message":<error>},"id":<id>}`

### Requirement: Error Response Format
The system SHALL return standardized JSON-RPC error responses for various error conditions.

#### Scenario: Parse error
- **WHEN** the request body is not valid JSON
- **THEN** the system SHALL return `{"jsonrpc":"2.0","error":{"code":-32700,"message":"Parse error"},"id":null}`

#### Scenario: Invalid request
- **WHEN** the request body is valid JSON but not a valid JSON-RPC request
- **THEN** the system SHALL return `{"jsonrpc":"2.0","error":{"code":-32600,"message":"Invalid Request"},"id":<id or null>}`
