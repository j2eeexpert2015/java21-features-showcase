package org.example.dto.rsavskem;

/**
 * Request DTO for KEM Step 3: Perform KEM Decapsulation
 * Used by POST /api/crypto/kem/decapsulate
 */
public record KemDecapsulateRequest(
        String privateKey,
        String encapsulation,
        String originalSecret  // For verification - compare derived secret with original
) {}
