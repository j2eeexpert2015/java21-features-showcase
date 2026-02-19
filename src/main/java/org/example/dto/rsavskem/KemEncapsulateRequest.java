package org.example.dto.rsavskem;

/**
 * Request DTO for KEM Step 2: Perform KEM Encapsulation
 * Used by POST /api/crypto/kem/encapsulate
 */
public record KemEncapsulateRequest(
        String publicKey
) {}
