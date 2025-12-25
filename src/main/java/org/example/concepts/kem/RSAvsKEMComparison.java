package org.example.concepts.kem;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Comprehensive comparison: RSA Key Transport vs KEM
 *
 * This class demonstrates both approaches to secure key establishment
 * and highlights the fundamental differences between them.
 */
public class RSAvsKEMComparison {

    private static final String SEPARATOR = "=".repeat(80);
    private static final String MESSAGE = "Payment confirmed: $1,299.00 - Order #12345";

    public static void main(String[] args) throws Exception {
        System.out.println("\n" + SEPARATOR);
        System.out.println("SECURE KEY ESTABLISHMENT: RSA KEY TRANSPORT vs KEM");
        System.out.println(SEPARATOR + "\n");

        // Demo 1: RSA Key Transport (Traditional Method)
        demonstrateRSAKeyTransport();

        System.out.println("\n" + SEPARATOR);
        System.out.println();

        // Demo 2: KEM (Modern Method)
        demonstrateKEM();

    }

    /**
     * RSA KEY TRANSPORT DEMONSTRATION
     *
     * Traditional method where:
     * - A symmetric key is generated
     * - The key is encrypted with RSA
     * - The encrypted key is transmitted
     */
    private static void demonstrateRSAKeyTransport() throws Exception {
        System.out.println("🔴 METHOD 1: RSA KEY TRANSPORT (Traditional)");
        System.out.println("-".repeat(80));
        System.out.println("Approach: Generate symmetric key → Encrypt it → Transmit encrypted key\n");

        // ============================================================
        // STEP 1: RECEIVER SETUP - Generate RSA Key Pair
        // ============================================================
        System.out.println("STEP 1: Receiver generates RSA key pair");
        KeyPairGenerator rsaKpg = KeyPairGenerator.getInstance("RSA");
        rsaKpg.initialize(2048);
        KeyPair receiverRSAKeys = rsaKpg.generateKeyPair();

        System.out.println("  ✅ RSA Public Key:  " +
                receiverRSAKeys.getPublic().getAlgorithm() +
                " (" + getKeySize(receiverRSAKeys.getPublic()) + " bits)");
        System.out.println("  ✅ RSA Private Key: " +
                receiverRSAKeys.getPrivate().getAlgorithm() +
                " (" + getKeySize(receiverRSAKeys.getPrivate()) + " bits)");
        System.out.println();

        // ============================================================
        // STEP 2: SENDER - Generate Random Symmetric Key
        // ============================================================
        System.out.println("STEP 2: Sender generates random AES symmetric key");
        KeyGenerator aesKeyGen = KeyGenerator.getInstance("AES");
        aesKeyGen.init(256);
        SecretKey originalAESKey = aesKeyGen.generateKey();

        byte[] originalKeyBytes = originalAESKey.getEncoded();
        System.out.println("  ✅ Generated AES Key: " +
                bytesToHex(originalKeyBytes, 16) + "...");
        System.out.println("     (256 bits = 32 bytes)");
        System.out.println("     ⚠️  THIS KEY WILL BE ENCRYPTED AND TRANSMITTED");
        System.out.println();

        // ============================================================
        // STEP 3: SENDER - Encrypt AES Key with RSA (+ PADDING)
        // ============================================================
        System.out.println("STEP 3: Sender encrypts AES key using receiver's RSA public key");

        // Configure RSA with OAEP padding
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
        );

        Cipher rsaEncryptCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaEncryptCipher.init(Cipher.ENCRYPT_MODE, receiverRSAKeys.getPublic(), oaepParams);

        byte[] encryptedAESKey = rsaEncryptCipher.doFinal(originalKeyBytes);

        System.out.println("  ✅ Padding Scheme: OAEP (SHA-256 + MGF1)");
        System.out.println("  ✅ Encrypted AES Key: " +
                bytesToHex(encryptedAESKey, 16) + "...");
        System.out.println("     (" + encryptedAESKey.length + " bytes)");
        System.out.println();

        // ============================================================
        // STEP 4: NETWORK TRANSMISSION
        // ============================================================
        System.out.println("STEP 4: Transmit encrypted key over network");
        System.out.println("  📡 NETWORK TRANSMISSION:");
        System.out.println("     Size: " + encryptedAESKey.length + " bytes");
        System.out.println("     🔴 CONTAINS: The actual AES key (encrypted)");
        System.out.println("     ⚠️  If padding oracle attack succeeds → key compromised");
        System.out.println();

        // ============================================================
        // STEP 5: RECEIVER - Decrypt AES Key
        // ============================================================
        System.out.println("STEP 5: Receiver decrypts AES key using RSA private key");

        Cipher rsaDecryptCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaDecryptCipher.init(Cipher.DECRYPT_MODE, receiverRSAKeys.getPrivate(), oaepParams);

        byte[] decryptedAESKeyBytes = rsaDecryptCipher.doFinal(encryptedAESKey);
        SecretKey recoveredAESKey = new SecretKeySpec(decryptedAESKeyBytes, "AES");

        System.out.println("  ✅ Decrypted AES Key: " +
                bytesToHex(decryptedAESKeyBytes, 16) + "...");
        System.out.println();

        // ============================================================
        // STEP 6: VERIFY - Both sides have the same key
        // ============================================================
        boolean rsaKeysMatch = Arrays.equals(originalKeyBytes, decryptedAESKeyBytes);
        System.out.println("STEP 6: Verification");
        System.out.println("  " + (rsaKeysMatch ? "✅" : "❌") +
                " Keys match: " + rsaKeysMatch);
        System.out.println();

        // ============================================================
        // STEP 7: USE THE KEY - Encrypt actual message
        // ============================================================
        System.out.println("STEP 7: Use established key to encrypt actual message");
        encryptMessageWithAES(recoveredAESKey, MESSAGE);
    }

    /**
     * KEM DEMONSTRATION
     *
     * Modern method where:
     * - A shared secret is derived (not generated separately)
     * - Only encapsulation data is transmitted
     * - The shared secret never travels the network
     */
    private static void demonstrateKEM() throws Exception {
        System.out.println("🟢 METHOD 2: KEM (Key Encapsulation Mechanism - Modern)");
        System.out.println("-".repeat(80));
        System.out.println("Approach: Derive shared secret → Transmit encapsulation (NOT the key)\n");

        // ============================================================
        // STEP 1: RECEIVER SETUP - Generate X25519 Key Pair
        // ============================================================
        System.out.println("STEP 1: Receiver generates X25519 key pair");
        KeyPairGenerator x25519Kpg = KeyPairGenerator.getInstance("X25519");
        KeyPair receiverX25519Keys = x25519Kpg.generateKeyPair();

        System.out.println("  ✅ X25519 Public Key:  " +
                receiverX25519Keys.getPublic().getAlgorithm() +
                " (256 bits)");
        System.out.println("  ✅ X25519 Private Key: " +
                receiverX25519Keys.getPrivate().getAlgorithm() +
                " (256 bits)");
        System.out.println();

        // ============================================================
        // STEP 2: SENDER - KEM Encapsulation
        // ============================================================
        System.out.println("STEP 2: Sender performs KEM encapsulation");
        System.out.println("  (Using receiver's public key + internal randomness)");

        KEM kem = KEM.getInstance("DHKEM");
        KEM.Encapsulator encapsulator = kem.newEncapsulator(receiverX25519Keys.getPublic());
        KEM.Encapsulated encapsulated = encapsulator.encapsulate();

        // Extract the TWO outputs
        SecretKey senderSharedSecret = encapsulated.key();
        byte[] encapsulation = encapsulated.encapsulation();

        byte[] senderSecretBytes = senderSharedSecret.getEncoded();

        System.out.println("\n  KEM produces TWO outputs:");
        System.out.println();
        System.out.println("  OUTPUT 1 - Shared Secret (KEPT LOCAL, NEVER SENT):");
        System.out.println("    ✅ Shared Secret: " + bytesToHex(senderSecretBytes, 16) + "...");
        System.out.println("       (" + senderSecretBytes.length + " bytes)");
        System.out.println("       🟢 THIS IS KEPT LOCAL - NEVER TRANSMITTED");
        System.out.println();

        System.out.println("  OUTPUT 2 - Encapsulation (Safe to transmit):");
        System.out.println("    ✅ Encapsulation: " + bytesToHex(encapsulation, 16) + "...");
        System.out.println("       (" + encapsulation.length + " bytes)");
        System.out.println("       📦 For DHKEM/X25519: This is sender's ephemeral public key");
        System.out.println("       🟢 SAFE TO SEND - Contains NO key material");
        System.out.println();

        // ============================================================
        // STEP 3: NETWORK TRANSMISSION
        // ============================================================
        System.out.println("STEP 3: Transmit encapsulation over network");
        System.out.println("  📡 NETWORK TRANSMISSION:");
        System.out.println("     Size: " + encapsulation.length + " bytes");
        System.out.println("     🟢 CONTAINS: Public derivation data (ephemeral public key)");
        System.out.println("     ✅ NO key material - attacker learns nothing useful");
        System.out.println("     ✅ No padding schemes needed");
        System.out.println();

        // ============================================================
        // STEP 4: RECEIVER - KEM Decapsulation
        // ============================================================
        System.out.println("STEP 4: Receiver performs KEM decapsulation");
        System.out.println("  (Using private key + received encapsulation)");

        KEM.Decapsulator decapsulator = kem.newDecapsulator(receiverX25519Keys.getPrivate());
        SecretKey receiverSharedSecret = decapsulator.decapsulate(encapsulation);

        byte[] receiverSecretBytes = receiverSharedSecret.getEncoded();

        System.out.println("  ✅ Derived Shared Secret: " +
                bytesToHex(receiverSecretBytes, 16) + "...");
        System.out.println("     (" + receiverSecretBytes.length + " bytes)");
        System.out.println("     🟢 DERIVED LOCALLY - Was never transmitted");
        System.out.println();

        // ============================================================
        // STEP 5: VERIFY - Both sides have the same shared secret
        // ============================================================
        boolean kemKeysMatch = Arrays.equals(senderSecretBytes, receiverSecretBytes);
        System.out.println("STEP 5: Verification");
        System.out.println("  " + (kemKeysMatch ? "✅" : "❌") +
                " Shared secrets match: " + kemKeysMatch);
        System.out.println();

        // ============================================================
        // STEP 6: DERIVE AES KEY - Use KDF in production
        // ============================================================
        System.out.println("STEP 6: Derive AES key from shared secret");
        System.out.println("  ⚠️  In production: Use HKDF or similar KDF");
        System.out.println("  ⚠️  This demo: Direct truncation (for simplicity only)");

        byte[] aesKeyBytes = Arrays.copyOf(receiverSecretBytes, 32); // 256 bits
        SecretKey derivedAESKey = new SecretKeySpec(aesKeyBytes, "AES");

        System.out.println("  ✅ Derived AES Key: " + bytesToHex(aesKeyBytes, 16) + "...");
        System.out.println();

        // ============================================================
        // STEP 7: USE THE KEY - Encrypt actual message
        // ============================================================
        System.out.println("STEP 7: Use derived key to encrypt actual message");
        encryptMessageWithAES(derivedAESKey, MESSAGE);
    }

    /**
     * Helper: Encrypt a message with AES-GCM
     */
    private static void encryptMessageWithAES(SecretKey aesKey, String message) throws Exception {
        // Generate random IV
        byte[] iv = new byte[12];
        SecureRandom.getInstanceStrong().nextBytes(iv);

        // Encrypt
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
        byte[] ciphertext = cipher.doFinal(message.getBytes());

        System.out.println("  ✅ Message encrypted with AES-GCM");
        System.out.println("     Original: \"" + message + "\"");
        System.out.println("     Ciphertext: " + bytesToHex(ciphertext, 32) + "...");
        System.out.println("     (" + ciphertext.length + " bytes)");

        // Decrypt to verify
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
        String decrypted = new String(cipher.doFinal(ciphertext));

        System.out.println("  ✅ Decrypted: \"" + decrypted + "\"");
        System.out.println("  ✅ Encryption/Decryption successful!");
    }



    // ============================================================
    // UTILITY METHODS
    // ============================================================

    private static String bytesToHex(byte[] bytes, int limit) {
        StringBuilder sb = new StringBuilder();
        int len = Math.min(bytes.length, limit);
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02x", bytes[i]));
        }
        return sb.toString();
    }

    private static int getKeySize(Key key) {
        if (key instanceof java.security.interfaces.RSAKey) {
            return ((java.security.interfaces.RSAKey) key).getModulus().bitLength();
        }
        return -1;
    }
}
