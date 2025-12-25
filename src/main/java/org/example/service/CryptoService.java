package org.example.service;

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
 * @author Learning from Experience
 */
@Service
public class CryptoService {

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
     *
     * @return KeyPair containing RSA public and private keys
     * @throws NoSuchAlgorithmException if RSA algorithm is not available
     */
    public KeyPair generateRsaKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGenerator.initialize(RSA_KEY_SIZE);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Step 2: Generate Random AES Key (256 bits)
     *
     * @return SecretKey for AES encryption
     * @throws NoSuchAlgorithmException if AES algorithm is not available
     */
    public SecretKey generateAesKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGenerator.init(AES_KEY_SIZE);
        return keyGenerator.generateKey();
    }

    /**
     * Step 3: Encrypt AES Key with RSA Public Key (OAEP padding)
     *
     * @param rsaPublicKeyBase64 RSA public key in Base64 format
     * @param aesKeyBase64 AES key in Base64 format
     * @return Encrypted AES key bytes
     * @throws Exception if encryption fails
     */
    public byte[] encryptAesKeyWithRsa(String rsaPublicKeyBase64, String aesKeyBase64) throws Exception {
        // Decode keys from Base64
        PublicKey publicKey = decodeRsaPublicKey(rsaPublicKeyBase64);
        byte[] aesKeyBytes = Base64.getDecoder().decode(aesKeyBase64);

        // Configure OAEP padding (SHA-256 + MGF1)
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
        );

        // Encrypt AES key with RSA
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams);

        return cipher.doFinal(aesKeyBytes);
    }

    /**
     * Step 4: Decrypt AES Key with RSA Private Key
     *
     * @param rsaPrivateKeyBase64 RSA private key in Base64 format
     * @param encryptedKeyBase64 Encrypted AES key in Base64 format
     * @return Decrypted AES key bytes
     * @throws Exception if decryption fails
     */
    public byte[] decryptAesKeyWithRsa(String rsaPrivateKeyBase64, String encryptedKeyBase64) throws Exception {
        // Decode keys from Base64
        PrivateKey privateKey = decodeRsaPrivateKey(rsaPrivateKeyBase64);
        byte[] encryptedKey = Base64.getDecoder().decode(encryptedKeyBase64);

        // Configure OAEP padding (SHA-256 + MGF1)
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
        );

        // Decrypt with RSA
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);

        return cipher.doFinal(encryptedKey);
    }

    /**
     * Verify that two AES keys match
     *
     * @param originalKeyBase64 Original AES key in Base64
     * @param decryptedKeyBase64 Decrypted AES key in Base64
     * @return true if keys match, false otherwise
     */
    public boolean verifyAesKeysMatch(String originalKeyBase64, String decryptedKeyBase64) {
        byte[] original = Base64.getDecoder().decode(originalKeyBase64);
        byte[] decrypted = Base64.getDecoder().decode(decryptedKeyBase64);
        return Arrays.equals(original, decrypted);
    }

    // ============================================================================
    // KEM (KEY ENCAPSULATION MECHANISM) OPERATIONS
    // ============================================================================

    /**
     * Step 1: Generate X25519 Key Pair for KEM
     *
     * @return KeyPair containing X25519 public and private keys
     * @throws NoSuchAlgorithmException if X25519 algorithm is not available
     */
    public KeyPair generateKemKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(X25519_ALGORITHM);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Step 2: KEM Encapsulation
     * Produces: (1) Shared Secret (kept local), (2) Encapsulation (transmitted)
     *
     * @param kemPublicKeyBase64 X25519 public key in Base64 format
     * @return KEM.Encapsulated containing shared secret and encapsulation
     * @throws Exception if encapsulation fails
     */
    public KEM.Encapsulated performKemEncapsulation(String kemPublicKeyBase64) throws Exception {
        // Decode public key from Base64
        PublicKey publicKey = decodeX25519PublicKey(kemPublicKeyBase64);

        // Get KEM instance
        KEM kem = KEM.getInstance(KEM_ALGORITHM);

        // Create encapsulator and perform encapsulation
        KEM.Encapsulator encapsulator = kem.newEncapsulator(publicKey);
        return encapsulator.encapsulate();
    }

    /**
     * Step 3: KEM Decapsulation
     * Derives the same shared secret using private key + encapsulation
     *
     * @param kemPrivateKeyBase64 X25519 private key in Base64 format
     * @param encapsulationBase64 Encapsulation data in Base64 format
     * @return SecretKey - the derived shared secret
     * @throws Exception if decapsulation fails
     */
    public SecretKey performKemDecapsulation(String kemPrivateKeyBase64, String encapsulationBase64) throws Exception {
        // Decode private key and encapsulation from Base64
        PrivateKey privateKey = decodeX25519PrivateKey(kemPrivateKeyBase64);
        byte[] encapsulation = Base64.getDecoder().decode(encapsulationBase64);

        // Get KEM instance
        KEM kem = KEM.getInstance(KEM_ALGORITHM);

        // Create decapsulator and perform decapsulation
        KEM.Decapsulator decapsulator = kem.newDecapsulator(privateKey);
        return decapsulator.decapsulate(encapsulation);
    }

    /**
     * Verify that two shared secrets match
     *
     * @param originalSecretBase64 Original shared secret in Base64
     * @param derivedSecretBase64 Derived shared secret in Base64
     * @return true if secrets match, false otherwise
     */
    public boolean verifySecretsMatch(String originalSecretBase64, String derivedSecretBase64) {
        byte[] original = Base64.getDecoder().decode(originalSecretBase64);
        byte[] derived = Base64.getDecoder().decode(derivedSecretBase64);
        return Arrays.equals(original, derived);
    }

    /**
     * Derive AES key from KEM shared secret
     * Note: In production, use HKDF. This is simplified for demo.
     *
     * @param sharedSecretBase64 Shared secret in Base64 format
     * @return SecretKey for AES encryption
     */
    public SecretKey deriveAesKeyFromSharedSecret(String sharedSecretBase64) {
        byte[] sharedSecretBytes = Base64.getDecoder().decode(sharedSecretBase64);

        // Simple derivation: truncate to 256 bits (32 bytes)
        // TODO: In production, use HKDF-SHA256
        byte[] aesKeyBytes = Arrays.copyOf(sharedSecretBytes, 32); // 256 bits

        return new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);
    }

    // ============================================================================
    // MESSAGE ENCRYPTION/DECRYPTION (SHARED BY BOTH METHODS)
    // ============================================================================

    /**
     * Encrypt a message using AES-GCM
     *
     * @param aesKeyBase64 AES key in Base64 format
     * @param message Plain text message to encrypt
     * @return EncryptionResult containing IV and ciphertext
     * @throws Exception if encryption fails
     */
    public EncryptionResult encryptMessage(String aesKeyBase64, String message) throws Exception {
        // Decode AES key
        byte[] aesKeyBytes = Base64.getDecoder().decode(aesKeyBase64);
        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);

        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = SecureRandom.getInstanceStrong();
        random.nextBytes(iv);

        // Encrypt with AES-GCM
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, parameterSpec);

        byte[] ciphertext = cipher.doFinal(message.getBytes());

        return new EncryptionResult(iv, ciphertext);
    }

    /**
     * Decrypt a message using AES-GCM
     *
     * @param aesKeyBase64 AES key in Base64 format
     * @param ivBase64 IV in Base64 format
     * @param ciphertextBase64 Ciphertext in Base64 format
     * @return Decrypted plain text message
     * @throws Exception if decryption fails
     */
    public String decryptMessage(String aesKeyBase64, String ivBase64, String ciphertextBase64) throws Exception {
        // Decode parameters
        byte[] aesKeyBytes = Base64.getDecoder().decode(aesKeyBase64);
        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);
        byte[] iv = Base64.getDecoder().decode(ivBase64);
        byte[] ciphertext = Base64.getDecoder().decode(ciphertextBase64);

        // Decrypt with AES-GCM
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, parameterSpec);

        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext);
    }

    // ============================================================================
    // UTILITY METHODS - KEY ENCODING/DECODING
    // ============================================================================

    /**
     * Decode RSA public key from Base64
     */
    private PublicKey decodeRsaPublicKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return keyFactory.generatePublic(spec);
    }

    /**
     * Decode RSA private key from Base64
     */
    private PrivateKey decodeRsaPrivateKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return keyFactory.generatePrivate(spec);
    }

    /**
     * Decode X25519 public key from Base64
     */
    private PublicKey decodeX25519PublicKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(X25519_ALGORITHM);
        return keyFactory.generatePublic(spec);
    }

    /**
     * Decode X25519 private key from Base64
     */
    private PrivateKey decodeX25519PrivateKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(X25519_ALGORITHM);
        return keyFactory.generatePrivate(spec);
    }

    /**
     * Convert key to Base64 string
     */
    public String keyToBase64(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Convert byte array to Base64 string
     */
    public String bytesToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Convert byte array to hex string
     */
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

    /**
     * Container for encryption results (IV + Ciphertext)
     */
    public static class EncryptionResult {
        private final byte[] iv;
        private final byte[] ciphertext;

        public EncryptionResult(byte[] iv, byte[] ciphertext) {
            this.iv = iv;
            this.ciphertext = ciphertext;
        }

        public byte[] getIv() {
            return iv;
        }

        public byte[] getCiphertext() {
            return ciphertext;
        }
    }
}
