## ADDED Requirements

### Requirement: Sensitive Configuration via Environment Variables
The system SHALL support loading sensitive configuration values (database password, API keys) from environment variables rather than plaintext files.

#### Scenario: Environment variable configuration
- **WHEN** the application is started with environment variables set for sensitive fields
- **THEN** the system SHALL use those values for configuration and NOT fall back to plaintext values

### Requirement: Encrypted Configuration Values
The system SHALL support encrypted configuration values that are decrypted at runtime.

#### Scenario: Encrypted database password
- **WHEN** the database password is stored as an encrypted value in configuration
- **THEN** the system SHALL decrypt it using the configured encryption key before use

### Requirement: Secure Logging
The system SHALL NOT log sensitive configuration values or API keys.

#### Scenario: Sensitive value in logs
- **WHEN** a request includes an API Key in the header
- **THEN** the system SHALL mask or omit the API Key from all log output
