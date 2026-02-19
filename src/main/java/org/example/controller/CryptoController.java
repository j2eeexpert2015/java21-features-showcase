package org.example.controller;

import org.example.dto.rsavskem.*;
import org.example.service.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.KEM;
import javax.crypto.SecretKey;
import java.security.KeyPair;

/**
 * REST Controller for RSA vs KEM Comparison Demo
 * Provides endpoints for both RSA Key Transport and KEM operations
 */
@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

    private static final Logger log = LoggerFactory.getLogger(CryptoController.class);

    private final CryptoService cryptoService;

    public CryptoController(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    // ============================================================================
    // RSA KEY TRANSPORT ENDPOINTS
    // ============================================================================

    @GetMapping("/rsa/generate-keypair")
    public ResponseEntity<CryptoResponse> generateRsaKeyPair() {
        log.info("==> RSA: Generate Key Pair request");
        try {
            // Call service - all crypto happens here
            KeyPair keyPair = cryptoService.generateRsaKeyPair();

            // Build response with metadata for debug panel
            CryptoResponse response = CryptoResponse.success(
                            "rsa-generate-keypair",
                            "RSA key pair generated successfully"
                    )
                    .addData("publicKey", cryptoService.keyToBase64(keyPair.getPublic()))
                    .addData("privateKey", cryptoService.keyToBase64(keyPair.getPrivate()))
                    .addData("keySize", 2048)
                    .addData("algorithm", "RSA");

            log.info("<== RSA: Key pair generated (2048 bits)");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("<== RSA: Key pair generation failed - {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CryptoResponse.error("rsa-generate-keypair", e.getMessage()));
        }
    }

    @GetMapping("/rsa/generate-aes-key")
    public ResponseEntity<CryptoResponse> generateAesKey() {
        log.info("==> RSA: Generate AES Key request");
        try {
            SecretKey aesKey = cryptoService.generateAesKey();

            CryptoResponse response = CryptoResponse.success(
                            "rsa-generate-aes-key",
                            "AES key generated successfully"
                    )
                    .addData("aesKey", cryptoService.keyToBase64(aesKey))
                    .addData("keySize", 256)
                    .addData("algorithm", "AES");

            log.info("<== RSA: AES key generated (256 bits / 32 bytes)");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("<== RSA: AES key generation failed - {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CryptoResponse.error("rsa-generate-aes-key", e.getMessage()));
        }
    }

    @PostMapping("/rsa/encrypt-aes-key")
    public ResponseEntity<CryptoResponse> encryptAesKey(@RequestBody RsaEncryptRequest request) {
        log.info("==> RSA: Encrypt AES Key request");
        try {
            // Direct field access - type-safe, no manual parsing
            byte[] encryptedKey = cryptoService.encryptAesKeyWithRsa(
                    request.publicKey(),
                    request.aesKey()
            );

            CryptoResponse response = CryptoResponse.success(
                            "rsa-encrypt-aes-key",
                            "AES key encrypted successfully"
                    )
                    .addData("encryptedKey", cryptoService.bytesToBase64(encryptedKey))
                    .addData("sizeInBytes", encryptedKey.length)
                    .addData("paddingScheme", "OAEP (SHA-256 + MGF1)");

            log.info("<== RSA: AES key encrypted ({} bytes transmitted - contains key material)", encryptedKey.length);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("<== RSA: Encryption failed - {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CryptoResponse.error("rsa-encrypt-aes-key", e.getMessage()));
        }
    }

    @PostMapping("/rsa/decrypt-aes-key")
    public ResponseEntity<CryptoResponse> decryptAesKey(@RequestBody RsaDecryptRequest request) {
        log.info("==> RSA: Decrypt AES Key request");
        try {
            // Decrypt and verify against original
            byte[] decryptedKey = cryptoService.decryptAesKeyWithRsa(
                    request.privateKey(),
                    request.encryptedKey()
            );
            String decryptedKeyBase64 = cryptoService.bytesToBase64(decryptedKey);

            boolean keysMatch = cryptoService.verifyAesKeysMatch(
                    request.originalKey(),
                    decryptedKeyBase64
            );

            CryptoResponse response = CryptoResponse.success(
                            "rsa-decrypt-aes-key",
                            "AES key decrypted successfully"
                    )
                    .addData("decryptedKey", decryptedKeyBase64)
                    .addData("keysMatch", keysMatch);

            log.info("<== RSA: AES key decrypted (keysMatch: {})", keysMatch);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("<== RSA: Decryption failed - {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CryptoResponse.error("rsa-decrypt-aes-key", e.getMessage()));
        }
    }

    // ============================================================================
    // KEM ENDPOINTS
    // ============================================================================

    @GetMapping("/kem/generate-keypair")
    public ResponseEntity<CryptoResponse> generateKemKeyPair() {
        log.info("==> KEM: Generate X25519 Key Pair request");
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

            log.info("<== KEM: X25519 key pair generated");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("<== KEM: Key pair generation failed - {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CryptoResponse.error("kem-generate-keypair", e.getMessage()));
        }
    }

    @PostMapping("/kem/encapsulate")
    public ResponseEntity<CryptoResponse> kemEncapsulate(@RequestBody KemEncapsulateRequest request) {
        log.info("==> KEM: Encapsulation request");
        try {
            // Produces both shared secret (local) and encapsulation (transmitted)
            KEM.Encapsulated encapsulated = cryptoService.performKemEncapsulation(request.publicKey());

            CryptoResponse response = CryptoResponse.success(
                            "kem-encapsulate",
                            "KEM encapsulation completed successfully"
                    )
                    .addData("sharedSecret", cryptoService.keyToBase64(encapsulated.key()))
                    .addData("encapsulation", cryptoService.bytesToBase64(encapsulated.encapsulation()))
                    .addData("encapsulationSize", encapsulated.encapsulation().length);

            log.info("<== KEM: Encapsulation complete ({} bytes transmitted - NO key material)",
                    encapsulated.encapsulation().length);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("<== KEM: Encapsulation failed - {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CryptoResponse.error("kem-encapsulate", e.getMessage()));
        }
    }

    @PostMapping("/kem/decapsulate")
    public ResponseEntity<CryptoResponse> kemDecapsulate(@RequestBody KemDecapsulateRequest request) {
        log.info("==> KEM: Decapsulation request");
        try {
            // Derive secret locally using private key + encapsulation
            SecretKey derivedSecret = cryptoService.performKemDecapsulation(
                    request.privateKey(),
                    request.encapsulation()
            );
            String derivedSecretBase64 = cryptoService.keyToBase64(derivedSecret);

            boolean secretsMatch = cryptoService.verifySecretsMatch(
                    request.originalSecret(),
                    derivedSecretBase64
            );

            CryptoResponse response = CryptoResponse.success(
                            "kem-decapsulate",
                            "KEM decapsulation completed successfully"
                    )
                    .addData("derivedSecret", derivedSecretBase64)
                    .addData("secretsMatch", secretsMatch);

            log.info("<== KEM: Decapsulation complete (secretsMatch: {}, derived locally - never transmitted)", secretsMatch);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("<== KEM: Decapsulation failed - {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CryptoResponse.error("kem-decapsulate", e.getMessage()));
        }
    }

    // ============================================================================
    // MESSAGE ENCRYPTION ENDPOINTS (SHARED)
    // ============================================================================

    @PostMapping("/encrypt-message")
    public ResponseEntity<CryptoResponse> encryptMessage(@RequestBody EncryptMessageRequest request) {
        log.info("==> Encrypt message request");
        try {
            // Works with any AES key - from RSA or KEM
            CryptoService.EncryptionResult result = cryptoService.encryptMessage(
                    request.aesKey(),
                    request.message()
            );

            CryptoResponse response = CryptoResponse.success(
                            "encrypt-message",
                            "Message encrypted successfully"
                    )
                    .addData("ciphertext", cryptoService.bytesToBase64(result.ciphertext()))
                    .addData("iv", cryptoService.bytesToBase64(result.iv()))
                    .addData("algorithm", "AES-GCM");

            log.info("<== Message encrypted (AES-GCM)");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("<== Message encryption failed - {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CryptoResponse.error("encrypt-message", e.getMessage()));
        }
    }

    @PostMapping("/decrypt-message")
    public ResponseEntity<CryptoResponse> decryptMessage(@RequestBody DecryptMessageRequest request) {
        log.info("==> Decrypt message request");
        try {
            String plaintext = cryptoService.decryptMessage(
                    request.aesKey(),
                    request.iv(),
                    request.ciphertext()
            );

            CryptoResponse response = CryptoResponse.success(
                            "decrypt-message",
                            "Message decrypted successfully"
                    )
                    .addData("plaintext", plaintext);

            log.info("<== Message decrypted (AES-GCM)");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("<== Message decryption failed - {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CryptoResponse.error("decrypt-message", e.getMessage()));
        }
    }
}