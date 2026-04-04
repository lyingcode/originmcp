package com.bitsoft.originmcp.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-based encryption utility for securing sensitive configuration values.
 * Uses AES/GCM/NoPadding for authenticated encryption.
 */
@Component
public class AesEncryptor {

    private static final Logger log = LoggerFactory.getLogger(AesEncryptor.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // bits

    @Value("${mcp.security.encrypt.enabled:true}")
    private boolean encryptionEnabled;

    @Value("${mcp.security.encrypt.key-env:MCP_ENCRYPT_KEY}")
    private String keyEnvName;

    private volatile SecretKeySpec secretKey;
    private volatile boolean initialized = false;

    /**
     * Initialize the encryptor with the key from environment variable.
     */
    public void initialize() {
        if (!encryptionEnabled) {
            log.info("Encryption is disabled");
            return;
        }

        String key = System.getenv(keyEnvName);
        if (key == null || key.isBlank()) {
            log.warn("Encryption key environment variable '{}' is not set. Encryption disabled.", keyEnvName);
            return;
        }

        try {
            // Derive a 256-bit key from the environment variable using SHA-256
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(key.getBytes(StandardCharsets.UTF_8));

            secretKey = new SecretKeySpec(keyBytes, "AES");
            initialized = true;
            log.info("AesEncryptor initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize AesEncryptor: {}", e.getMessage());
        }
    }

    /**
     * Check if encryption is available and enabled.
     */
    public boolean isInitialized() {
        return initialized && encryptionEnabled;
    }

    /**
     * Encrypt a plaintext value.
     *
     * @param plaintext The value to encrypt
     * @return Base64-encoded encrypted value (IV prepended), or original value if encryption fails
     */
    public String encrypt(String plaintext) {
        if (!isInitialized() || plaintext == null) {
            return plaintext;
        }

        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = SecureRandom.getInstanceStrong();
            random.nextBytes(iv);

            // Encrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage());
            return plaintext;
        }
    }

    /**
     * Decrypt an encrypted value.
     *
     * @param encryptedValue Base64-encoded encrypted value (IV prepended)
     * @return Decrypted plaintext, or original value if decryption fails
     */
    public String decrypt(String encryptedValue) {
        if (!isInitialized() || encryptedValue == null) {
            return encryptedValue;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedValue);

            // Extract IV
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            // Extract ciphertext
            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            // Decrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage());
            return encryptedValue;
        }
    }

    /**
     * Check if a value appears to be encrypted (starts with Base64-encoded IV).
     */
    public boolean isEncrypted(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            // Minimum length: GCM_IV_LENGTH (12) + at least 1 byte ciphertext
            return decoded.length > GCM_IV_LENGTH;
        } catch (Exception e) {
            return false;
        }
    }
}
