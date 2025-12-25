package org.example.controller;



import org.example.dto.rsavskem.CryptoRequest;
import org.example.dto.rsavskem.CryptoResponse;
import org.example.service.CryptoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.KEM;
import javax.crypto.SecretKey;
import java.security.KeyPair;

/**
 * REST Controller for RSA vs KEM Comparison Demo
 * Provides endpoints for both RSA Key Transport and KEM operations
 *
 * @author Learning from Experience
 */
@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

    private final CryptoService cryptoService;

    public CryptoController(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    // ============================================================================
    // RSA KEY TRANSPORT ENDPOINTS
    // ============================================================================

    /**
     * Step 1: Generate RSA Key Pair (2048 bits)
     *
     * GET /api/crypto/rsa/generate-keypair
     *
     * Response:
     * {
     *   "success": true,
     *   "operation": "rsa-generate-keypair",
     *   "message": "RSA key pair generated successfully",
     *   "data": {
     *     "publicKey": "MIIBIjANBgkqhki...",
     *     "privateKey": "MIIEvQIBADANBgk...",
     *     "keySize": 2048,
     *     "algorithm": "RSA"
     *   }
     * }
     */
    @GetMapping("/rsa/generate-keypair")
    public ResponseEntity<CryptoResponse> generateRsaKeyPair() {
        try {
            KeyPair keyPair = cryptoService.generateRsaKeyPair();

            CryptoResponse response = CryptoResponse.success(
                            "rsa-generate-keypair",
                            "RSA key pair generated successfully"
                    )
                    .addData("publicKey", cryptoService.keyToBase64(keyPair.getPublic()))
                    .addData("privateKey", cryptoService.keyToBase64(keyPair.getPrivate()))
                    .addData("keySize", 2048)
                    .addData("algorithm", "RSA");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CryptoResponse.error("rsa-generate-keypair", e.getMessage()));
        }
    }

    /**
     * Step 2: Generate Random AES Key (256 bits)
     *
     * GET /api/crypto/rsa/generate-aes-key
     *
     * Response:
     * {
     *   "success": true,
     *   "operation": "rsa-generate-aes-key",
     *   "message": "AES key generated successfully",
     *   "data": {
     *     "aesKey": "7a3f2e8d9c1b4a5e...",
     *     "keySize": 256,
     *     "algorithm": "AES"
     *   }
     * }
     */
    @GetMapping("/rsa/generate-aes-key")
    public ResponseEntity<CryptoResponse> generateAesKey() {
        try {
            SecretKey aesKey = cryptoService.generateAesKey();

            CryptoResponse response = CryptoResponse.success(
                            "rsa-generate-aes-key",
                            "AES key generated successfully"
                    )
                    .addData("aesKey", cryptoService.keyToBase64(aesKey))
                    .addData("keySize", 256)
                    .addData("algorithm", "AES");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CryptoResponse.error("rsa-generate-aes-key", e.getMessage()));
        }
    }

    /**
     * Step 3: Encrypt AES Key with RSA Public Key
     *
     * POST /api/crypto/rsa/encrypt-aes-key
     *
     * Request Body:
     * {
     *   "data": "{\"publicKey\":\"...\",\"aesKey\":\"...\"}"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "operation": "rsa-encrypt-aes-key",
     *   "message": "AES key encrypted successfully",
     *   "data": {
     *     "encryptedKey": "8e4d7c2a9f3b1e5d...",
     *     "sizeInBytes": 256,
     *     "paddingScheme": "OAEP (SHA-256 + MGF1)"
     *   }
     * }
     */
    @PostMapping("/rsa/encrypt-aes-key")
    public ResponseEntity<CryptoResponse> encryptAesKey(@RequestBody CryptoRequest request) {
        try {
            // Parse JSON data from request
            String publicKey = extractJsonField(request.getData(), "publicKey");
            String aesKey = extractJsonField(request.getData(), "aesKey");

            // Encrypt AES key with RSA
            byte[] encryptedKey = cryptoService.encryptAesKeyWithRsa(publicKey, aesKey);

            CryptoResponse response = CryptoResponse.success(
                            "rsa-encrypt-aes-key",
                            "AES key encrypted successfully"
                    )
                    .addData("encryptedKey", cryptoService.bytesToBase64(encryptedKey))
                    .addData("sizeInBytes", encryptedKey.length)
                    .addData("paddingScheme", "OAEP (SHA-256 + MGF1)");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CryptoResponse.error("rsa-encrypt-aes-key", e.getMessage()));
        }
    }

    /**
     * Step 4: Decrypt AES Key with RSA Private Key
     *
     * POST /api/crypto/rsa/decrypt-aes-key
     *
     * Request Body:
     * {
     *   "data": "{\"privateKey\":\"...\",\"encryptedKey\":\"...\",\"originalKey\":\"...\"}"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "operation": "rsa-decrypt-aes-key",
     *   "message": "AES key decrypted successfully",
     *   "data": {
     *     "decryptedKey": "7a3f2e8d9c1b4a5e...",
     *     "keysMatch": true
     *   }
     * }
     */
    @PostMapping("/rsa/decrypt-aes-key")
    public ResponseEntity<CryptoResponse> decryptAesKey(@RequestBody CryptoRequest request) {
        try {
            // Parse JSON data from request
            String privateKey = extractJsonField(request.getData(), "privateKey");
            String encryptedKey = extractJsonField(request.getData(), "encryptedKey");
            String originalKey = extractJsonField(request.getData(), "originalKey");

            // Decrypt AES key with RSA
            byte[] decryptedKey = cryptoService.decryptAesKeyWithRsa(privateKey, encryptedKey);
            String decryptedKeyBase64 = cryptoService.bytesToBase64(decryptedKey);

            // Verify keys match
            boolean keysMatch = cryptoService.verifyAesKeysMatch(originalKey, decryptedKeyBase64);

            CryptoResponse response = CryptoResponse.success(
                            "rsa-decrypt-aes-key",
                            "AES key decrypted successfully"
                    )
                    .addData("decryptedKey", decryptedKeyBase64)
                    .addData("keysMatch", keysMatch);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CryptoResponse.error("rsa-decrypt-aes-key", e.getMessage()));
        }
    }

    // ============================================================================
    // KEM ENDPOINTS
    // ============================================================================

    /**
     * Step 1: Generate X25519 Key Pair for KEM
     *
     * GET /api/crypto/kem/generate-keypair
     *
     * Response:
     * {
     *   "success": true,
     *   "operation": "kem-generate-keypair",
     *   "message": "X25519 key pair generated successfully",
     *   "data": {
     *     "publicKey": "MCowBQYDK2VuAyEA...",
     *     "privateKey": "MC4CAQAwBQYDK2Vu...",
     *     "keySize": 256,
     *     "algorithm": "X25519"
     *   }
     * }
     */
    @GetMapping("/kem/generate-keypair")
    public ResponseEntity<CryptoResponse> generateKemKeyPair() {
        try {
            KeyPair keyPair = cryptoService.generateKemKeyPair();

            CryptoResponse response = CryptoResponse.success(
                            "kem-generate-keypair",
                            "X25519 key pair generated successfully"
                    )
                    .addData("publicKey", cryptoService.keyToBase64(keyPair.getPublic()))
                    .addData("privateKey", cryptoService.keyToBase64(keyPair.getPrivate()))
                    .addData("keySize", 256)
                    .addData("algorithm", "X25519");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CryptoResponse.error("kem-generate-keypair", e.getMessage()));
        }
    }

    /**
     * Step 2: KEM Encapsulation
     *
     * POST /api/crypto/kem/encapsulate
     *
     * Request Body:
     * {
     *   "data": "{\"publicKey\":\"...\"}"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "operation": "kem-encapsulate",
     *   "message": "KEM encapsulation completed successfully",
     *   "data": {
     *     "sharedSecret": "3d7f9e2c8a1b4d6f...",
     *     "encapsulation": "9a7e3f2d8c1b5e4a...",
     *     "encapsulationSize": 32
     *   }
     * }
     */
    @PostMapping("/kem/encapsulate")
    public ResponseEntity<CryptoResponse> kemEncapsulate(@RequestBody CryptoRequest request) {
        try {
            // Parse JSON data from request
            String publicKey = extractJsonField(request.getData(), "publicKey");

            // Perform KEM encapsulation
            KEM.Encapsulated encapsulated = cryptoService.performKemEncapsulation(publicKey);

            CryptoResponse response = CryptoResponse.success(
                            "kem-encapsulate",
                            "KEM encapsulation completed successfully"
                    )
                    .addData("sharedSecret", cryptoService.keyToBase64(encapsulated.key()))
                    .addData("encapsulation", cryptoService.bytesToBase64(encapsulated.encapsulation()))
                    .addData("encapsulationSize", encapsulated.encapsulation().length);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CryptoResponse.error("kem-encapsulate", e.getMessage()));
        }
    }

    /**
     * Step 3: KEM Decapsulation
     *
     * POST /api/crypto/kem/decapsulate
     *
     * Request Body:
     * {
     *   "data": "{\"privateKey\":\"...\",\"encapsulation\":\"...\",\"originalSecret\":\"...\"}"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "operation": "kem-decapsulate",
     *   "message": "KEM decapsulation completed successfully",
     *   "data": {
     *     "derivedSecret": "3d7f9e2c8a1b4d6f...",
     *     "secretsMatch": true
     *   }
     * }
     */
    @PostMapping("/kem/decapsulate")
    public ResponseEntity<CryptoResponse> kemDecapsulate(@RequestBody CryptoRequest request) {
        try {
            // Parse JSON data from request
            String privateKey = extractJsonField(request.getData(), "privateKey");
            String encapsulation = extractJsonField(request.getData(), "encapsulation");
            String originalSecret = extractJsonField(request.getData(), "originalSecret");

            // Perform KEM decapsulation
            SecretKey derivedSecret = cryptoService.performKemDecapsulation(privateKey, encapsulation);
            String derivedSecretBase64 = cryptoService.keyToBase64(derivedSecret);

            // Verify secrets match
            boolean secretsMatch = cryptoService.verifySecretsMatch(originalSecret, derivedSecretBase64);

            CryptoResponse response = CryptoResponse.success(
                            "kem-decapsulate",
                            "KEM decapsulation completed successfully"
                    )
                    .addData("derivedSecret", derivedSecretBase64)
                    .addData("secretsMatch", secretsMatch);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CryptoResponse.error("kem-decapsulate", e.getMessage()));
        }
    }

    // ============================================================================
    // MESSAGE ENCRYPTION ENDPOINTS (SHARED)
    // ============================================================================

    /**
     * Encrypt a message using AES-GCM
     *
     * POST /api/crypto/encrypt-message
     *
     * Request Body:
     * {
     *   "message": "Hello Bob! Payment confirmed: $1,299.00",
     *   "data": "{\"aesKey\":\"...\"}"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "operation": "encrypt-message",
     *   "message": "Message encrypted successfully",
     *   "data": {
     *     "ciphertext": "5f8e3a2d9c7b1e4a...",
     *     "iv": "a7f3e9c1b2d4f6e8...",
     *     "algorithm": "AES-GCM"
     *   }
     * }
     */
    @PostMapping("/encrypt-message")
    public ResponseEntity<CryptoResponse> encryptMessage(@RequestBody CryptoRequest request) {
        try {
            // Parse request
            String message = request.getMessage();
            String aesKey = extractJsonField(request.getData(), "aesKey");

            // Encrypt message
            CryptoService.EncryptionResult result = cryptoService.encryptMessage(aesKey, message);

            CryptoResponse response = CryptoResponse.success(
                            "encrypt-message",
                            "Message encrypted successfully"
                    )
                    .addData("ciphertext", cryptoService.bytesToBase64(result.getCiphertext()))
                    .addData("iv", cryptoService.bytesToBase64(result.getIv()))
                    .addData("algorithm", "AES-GCM");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CryptoResponse.error("encrypt-message", e.getMessage()));
        }
    }

    /**
     * Decrypt a message using AES-GCM
     *
     * POST /api/crypto/decrypt-message
     *
     * Request Body:
     * {
     *   "data": "{\"aesKey\":\"...\",\"iv\":\"...\",\"ciphertext\":\"...\"}"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "operation": "decrypt-message",
     *   "message": "Message decrypted successfully",
     *   "data": {
     *     "plaintext": "Hello Bob! Payment confirmed: $1,299.00"
     *   }
     * }
     */
    @PostMapping("/decrypt-message")
    public ResponseEntity<CryptoResponse> decryptMessage(@RequestBody CryptoRequest request) {
        try {
            // Parse request
            String aesKey = extractJsonField(request.getData(), "aesKey");
            String iv = extractJsonField(request.getData(), "iv");
            String ciphertext = extractJsonField(request.getData(), "ciphertext");

            // Decrypt message
            String plaintext = cryptoService.decryptMessage(aesKey, iv, ciphertext);

            CryptoResponse response = CryptoResponse.success(
                            "decrypt-message",
                            "Message decrypted successfully"
                    )
                    .addData("plaintext", plaintext);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CryptoResponse.error("decrypt-message", e.getMessage()));
        }
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    /**
     * Simple JSON field extractor (for demo purposes)
     * In production, use Jackson ObjectMapper
     */
    private String extractJsonField(String json, String fieldName) {
        if (json == null) {
            throw new IllegalArgumentException("JSON data is null");
        }

        String searchKey = "\"" + fieldName + "\":\"";
        int startIndex = json.indexOf(searchKey);

        if (startIndex == -1) {
            throw new IllegalArgumentException("Field '" + fieldName + "' not found in JSON");
        }

        startIndex += searchKey.length();
        int endIndex = json.indexOf("\"", startIndex);

        if (endIndex == -1) {
            throw new IllegalArgumentException("Malformed JSON for field '" + fieldName + "'");
        }

        return json.substring(startIndex, endIndex);
    }
}
