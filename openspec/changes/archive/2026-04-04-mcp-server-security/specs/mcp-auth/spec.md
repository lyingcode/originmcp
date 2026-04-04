## ADDED Requirements

### Requirement: API Key Authentication
MCP Server SHALL verify the identity of each client using an API Key provided in the request header `Authorization: Bearer <api-key>`.

#### Scenario: Valid API Key
- **WHEN** a client sends a request with a valid API Key in the Authorization header
- **THEN** the server SHALL process the request and return the result

#### Scenario: Missing API Key
- **WHEN** a client sends a request without an Authorization header
- **THEN** the server SHALL return error code `UNAUTHORIZED` with message "API Key required"

#### Scenario: Invalid API Key
- **WHEN** a client sends a request with an invalid or expired API Key
- **THEN** the server SHALL return error code `UNAUTHORIZED` with message "Invalid API Key"

### Requirement: API Key Storage
The system SHALL store API Keys securely, supporting storage in database tables with hashed values.

#### Scenario: API Key stored as hash
- **WHEN** an API Key is stored in the system
- **THEN** the system SHALL store only a salted hash of the key, not the plaintext
