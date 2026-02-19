package org.example.dto.rsavskem;

/**
 * Request DTO for Message Encryption (shared by both RSA and KEM)
 * Used by POST /api/crypto/encrypt-message
 */
public record EncryptMessageRequest(
        String message,
        String aesKey
) {}
