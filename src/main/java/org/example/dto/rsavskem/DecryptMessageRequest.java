package org.example.dto.rsavskem;

/**
 * Request DTO for Message Decryption (shared by both RSA and KEM)
 * Used by POST /api/crypto/decrypt-message
 */
public record DecryptMessageRequest(
        String ciphertext,
        String iv,
        String aesKey
) {}
