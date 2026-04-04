## ADDED Requirements

### Requirement: Tool-Level Access Control
The system SHALL enforce access control at the individual tool level. Each authenticated client SHALL have an explicit list of tools they are permitted to invoke.

#### Scenario: Authorized tool invocation
- **WHEN** an authenticated client with permission for tool `weather.fetch` invokes that tool
- **THEN** the server SHALL execute the tool and return the result

#### Scenario: Unauthorized tool invocation
- **WHEN** an authenticated client without permission for tool `weather.fetch` attempts to invoke that tool
- **THEN** the server SHALL return error code `ACCESS_DENIED` with message "Tool 'weather.fetch' is not accessible to this client"

#### Scenario: Permission lookup
- **WHEN** a client makes a request
- **THEN** the server SHALL look up the client's permitted tools from the database or configuration

### Requirement: Default Deny Policy
The system SHALL deny access to any tool unless explicitly granted.

#### Scenario: No explicit permission
- **WHEN** a client has no explicit permission entry for any tool
- **THEN** the server SHALL deny access to all tools by default
