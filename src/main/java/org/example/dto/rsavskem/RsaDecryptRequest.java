package org.example.dto.rsavskem;

/**
 * Request DTO for RSA Step 4: Decrypt AES Key with RSA Private Key
 * Used by POST /api/crypto/rsa/decrypt-aes-key
 */
public record RsaDecryptRequest(
        String privateKey,
        String encryptedKey,
        String originalKey  // For verification - compare decrypted key with original
) {}
