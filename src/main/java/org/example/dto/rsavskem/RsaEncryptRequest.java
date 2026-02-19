package org.example.dto.rsavskem;

/**
 * Request DTO for RSA Step 3: Encrypt AES Key with RSA Public Key
 * Used by POST /api/crypto/rsa/encrypt-aes-key
 */
public record RsaEncryptRequest(
        String publicKey,
        String aesKey
) {}
