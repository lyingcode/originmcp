package com.bitsoft.originmcp.config;

import com.bitsoft.originmcp.security.AesEncryptor;
import com.bitsoft.originmcp.security.McpAuthenticator;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Initializes security components on application startup.
 */
@Configuration
public class SecurityInitializer {

    private static final Logger log = LoggerFactory.getLogger(SecurityInitializer.class);

    @Autowired
    private AesEncryptor aesEncryptor;

    @Autowired
    private McpAuthenticator authenticator;

    @Autowired
    private ConfigurableEnvironment environment;

    @Value("${mcp.security.enabled:true}")
    private boolean securityEnabled;

    @Value("${mcp.security.encrypt.db-password-env:DB_PASSWORD}")
    private String dbPasswordEnv;

    @PostConstruct
    public void initialize() {
        // Initialize AES encryptor
        aesEncryptor.initialize();

        // Initialize authenticator (loads API keys from database)
        if (securityEnabled) {
            authenticator.init();
        }

        // Override database password if encrypted password is provided via environment
        overrideDatabasePasswordIfNeeded();
    }

    /**
     * If DB_PASSWORD environment variable is set (encrypted or plaintext),
     * use it to override the datasource password.
     */
    private void overrideDatabasePasswordIfNeeded() {
        String dbPassword = System.getenv(dbPasswordEnv);
        if (dbPassword != null && !dbPassword.isBlank()) {
            // If the password looks encrypted (Base64 format with IV), decrypt it
            if (aesEncryptor.isEncrypted(dbPassword)) {
                String decrypted = aesEncryptor.decrypt(dbPassword);
                log.info("Using encrypted database password from environment");
                setProperty("spring.datasource.password", decrypted);
            } else {
                log.info("Using database password from environment");
                setProperty("spring.datasource.password", dbPassword);
            }
        }
    }

    private void setProperty(String key, String value) {
        Map<String, Object> props = new HashMap<>();
        props.put(key, value);
        MapPropertySource propertySource = new MapPropertySource("securityInit", props);
        environment.getPropertySources().addFirst(propertySource);
    }
}
