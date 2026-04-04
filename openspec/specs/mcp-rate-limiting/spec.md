## ADDED Requirements

### Requirement: Client Rate Limiting
The system SHALL enforce rate limits per authenticated client to prevent resource exhaustion.

#### Scenario: Request within rate limit
- **WHEN** a client with remaining quota makes a request
- **THEN** the server SHALL process the request and decrement the client's quota

#### Scenario: Rate limit exceeded
- **WHEN** a client exceeds their configured rate limit (e.g., 60 requests per minute)
- **THEN** the server SHALL return error code `RATE_LIMITED` with message "Rate limit exceeded. Retry after X seconds"

### Requirement: Rate Limit Configuration
The system SHALL support configurable rate limits per client or globally.

#### Scenario: Custom rate limit per client
- **WHEN** a client has a custom rate limit configured
- **THEN** the system SHALL enforce that client's specific limit

#### Scenario: Default rate limit
- **WHEN** a client has no custom rate limit configured
- **THEN** the system SHALL apply the default rate limit (60 requests per minute)
