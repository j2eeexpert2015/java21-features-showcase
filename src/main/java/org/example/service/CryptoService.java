package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KEM;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Unified cryptographic service for RSA Key Transport and KEM operations
 * All methods are stateless - frontend manages state
 *
 */
@Service
public class CryptoService {

    private static final Logger log = LoggerFactory.getLogger(CryptoService.class);

    // Constants
    private static final int RSA_KEY_SIZE = 2048;
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String KEM_ALGORITHM = "DHKEM";
    private static final String X25519_ALGORITHM = "X25519";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String AES_ALGORITHM = "AES";

    // ============================================================================
    // RSA KEY TRANSPORT OPERATIONS
    // ============================================================================

    /**
     * Step 1: Generate RSA Key Pair (2048 bits)
     */
    public KeyPair generateRsaKeyPair() throws NoSuchAlgorithmException {
        log.info("Generating RSA key pair ({} bits)", RSA_KEY_SIZE);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGenerator.initialize(RSA_KEY_SIZE);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        log.info("RSA key pair generated successfully");
        return keyPair;
    }

    /**
     * Step 2: Generate Random AES Key (256 bits)
     */
    public SecretKey generateAesKey() throws NoSuchAlgorithmException {
        log.info("Generating AES key ({} bits)", AES_KEY_SIZE);
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGenerator.init(AES_KEY_SIZE);
        SecretKey aesKey = keyGenerator.generateKey();
        log.info("AES key generated: {} bytes", aesKey.getEncoded().length);
        return aesKey;
    }

    /**
     * Step 3: Encrypt AES Key with RSA Public Key (OAEP padding)
     */
    public byte[] encryptAesKeyWithRsa(String rsaPublicKeyBase64, String aesKeyBase64) throws Exception {
        log.info("Encrypting AES key with RSA (OAEP padding: SHA-256 + MGF1)");

        PublicKey publicKey = decodeRsaPublicKey(rsaPublicKeyBase64);
        byte[] aesKeyBytes = Base64.getDecoder().decode(aesKeyBase64);

        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
        );

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams);

        byte[] encryptedKey = cipher.doFinal(aesKeyBytes);
        log.info("AES key encrypted: {} bytes input → {} bytes output (contains actual key material)",
                aesKeyBytes.length, encryptedKey.length);
        return encryptedKey;
    }

    /**
     * Step 4: Decrypt AES Key with RSA Private Key
     */
    public byte[] decryptAesKeyWithRsa(String rsaPrivateKeyBase64, String encryptedKeyBase64) throws Exception {
        log.info("Decrypting AES key with RSA private key");

        PrivateKey privateKey = decodeRsaPrivateKey(rsaPrivateKeyBase64);
        byte[] encryptedKey = Base64.getDecoder().decode(encryptedKeyBase64);

        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
        );

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);

        byte[] decryptedKey = cipher.doFinal(encryptedKey);
        log.info("AES key decrypted: {} bytes → {} bytes recovered", encryptedKey.length, decryptedKey.length);
        return decryptedKey;
    }

    /**
     * Verify that two AES keys match
     */
    public boolean verifyAesKeysMatch(String originalKeyBase64, String decryptedKeyBase64) {
        byte[] original = Base64.getDecoder().decode(originalKeyBase64);
        byte[] decrypted = Base64.getDecoder().decode(decryptedKeyBase64);
        boolean match = Arrays.equals(original, decrypted);
        log.info("AES key verification: {}", match ? "MATCH" : "MISMATCH");
        return match;
    }

    // ============================================================================
    // KEM (KEY ENCAPSULATION MECHANISM) OPERATIONS
    // ============================================================================

    /**
     * Step 1: Generate X25519 Key Pair for KEM
     */
    public KeyPair generateKemKeyPair() throws NoSuchAlgorithmException {
        log.info("Generating X25519 key pair for KEM");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(X25519_ALGORITHM);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        log.info("X25519 key pair generated successfully");
        return keyPair;
    }

    /**
     * Step 2: KEM Encapsulation
     * Produces: (1) Shared Secret (kept local), (2) Encapsulation (transmitted)
     */
    public KEM.Encapsulated performKemEncapsulation(String kemPublicKeyBase64) throws Exception {
        log.info("Performing KEM encapsulation (algorithm: {})", KEM_ALGORITHM);

        PublicKey publicKey = decodeX25519PublicKey(kemPublicKeyBase64);
        KEM kem = KEM.getInstance(KEM_ALGORITHM);
        KEM.Encapsulator encapsulator = kem.newEncapsulator(publicKey);
        KEM.Encapsulated encapsulated = encapsulator.encapsulate();

        log.info("KEM encapsulation complete: {} bytes transmitted (NO key material inside)",
                encapsulated.encapsulation().length);
        return encapsulated;
    }

    /**
     * Step 3: KEM Decapsulation
     * Derives the same shared secret using private key + encapsulation
     */
    public SecretKey performKemDecapsulation(String kemPrivateKeyBase64, String encapsulationBase64) throws Exception {
        log.info("Performing KEM decapsulation");

        PrivateKey privateKey = decodeX25519PrivateKey(kemPrivateKeyBase64);
        byte[] encapsulation = Base64.getDecoder().decode(encapsulationBase64);

        KEM kem = KEM.getInstance(KEM_ALGORITHM);
        KEM.Decapsulator decapsulator = kem.newDecapsulator(privateKey);
        SecretKey derivedSecret = decapsulator.decapsulate(encapsulation);

        log.info("KEM decapsulation complete: shared secret derived locally (never transmitted)");
        return derivedSecret;
    }

    /**
     * Verify that two shared secrets match
     */
    public boolean verifySecretsMatch(String originalSecretBase64, String derivedSecretBase64) {
        byte[] original = Base64.getDecoder().decode(originalSecretBase64);
        byte[] derived = Base64.getDecoder().decode(derivedSecretBase64);
        boolean match = Arrays.equals(original, derived);
        log.info("Shared secret verification: {}", match ? "MATCH" : "MISMATCH");
        return match;
    }

    /**
     * Derive AES key from KEM shared secret
     */
    public SecretKey deriveAesKeyFromSharedSecret(String sharedSecretBase64) {
        byte[] sharedSecretBytes = Base64.getDecoder().decode(sharedSecretBase64);
        byte[] aesKeyBytes = Arrays.copyOf(sharedSecretBytes, 32);
        log.debug("Derived AES key from shared secret ({} bytes)", aesKeyBytes.length);
        return new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);
    }

    // ============================================================================
    // MESSAGE ENCRYPTION/DECRYPTION (SHARED BY BOTH METHODS)
    // ============================================================================

    /**
     * Encrypt a message using AES-GCM
     */
    public EncryptionResult encryptMessage(String aesKeyBase64, String message) throws Exception {
        log.info("Encrypting message with AES-GCM ({} chars)", message.length());

        byte[] aesKeyBytes = Base64.getDecoder().decode(aesKeyBase64);
        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);

        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = SecureRandom.getInstanceStrong();
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, parameterSpec);

        byte[] ciphertext = cipher.doFinal(message.getBytes());
        log.info("Message encrypted: {} bytes ciphertext (IV: {} bytes)", ciphertext.length, iv.length);

        return new EncryptionResult(iv, ciphertext);
    }

    /**
     * Decrypt a message using AES-GCM
     */
    public String decryptMessage(String aesKeyBase64, String ivBase64, String ciphertextBase64) throws Exception {
        log.info("Decrypting message with AES-GCM");

        byte[] aesKeyBytes = Base64.getDecoder().decode(aesKeyBase64);
        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);
        byte[] iv = Base64.getDecoder().decode(ivBase64);
        byte[] ciphertext = Base64.getDecoder().decode(ciphertextBase64);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, parameterSpec);

        byte[] plaintext = cipher.doFinal(ciphertext);
        log.info("Message decrypted successfully ({} chars)", plaintext.length);

        return new String(plaintext);
    }

    // ============================================================================
    // UTILITY METHODS - KEY ENCODING/DECODING
    // ============================================================================

    private PublicKey decodeRsaPublicKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return keyFactory.generatePublic(spec);
    }

    private PrivateKey decodeRsaPrivateKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return keyFactory.generatePrivate(spec);
    }

    private PublicKey decodeX25519PublicKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(X25519_ALGORITHM);
        return keyFactory.generatePublic(spec);
    }

    private PrivateKey decodeX25519PrivateKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(X25519_ALGORITHM);
        return keyFactory.generatePrivate(spec);
    }

    public String keyToBase64(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public String bytesToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ============================================================================
    // INNER CLASS - ENCRYPTION RESULT
    // ============================================================================

    public record EncryptionResult(byte[] iv, byte[] ciphertext) {}
}