package org.example.concepts.kem;

import javax.crypto.Cipher;
import javax.crypto.KEM;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class KEMDemo {

    // Step 1: Generate Key Pair
    public static KeyPair generateKeyPair() throws Exception {
        System.out.println("=== Step 1: Generate Key Pair ===");

        /*
         * KeyPairGenerator.getInstance("X25519")
         *
         * X25519 is an elliptic curve algorithm designed for key agreement.
         *
         * - Faster than RSA
         * - Smaller key sizes (256-bit vs RSA's 2048-bit)
         * - Widely used in TLS 1.3, Signal, WhatsApp, SSH
         */
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("X25519");
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        System.out.println("Public Key Algorithm: " + keyPair.getPublic().getAlgorithm());
        System.out.println("Private Key Algorithm: " + keyPair.getPrivate().getAlgorithm());
        System.out.println("Key pair generated successfully.\n");

        return keyPair;
    }

    // Step 2: Sender uses KEM + public key → derives shared secret + encapsulation
    public static KEM.Encapsulated encapsulate(PublicKey publicKey) throws Exception {
        System.out.println("=== Step 2: Sender Encapsulation ===");

        /*
         * KEM.getInstance("DHKEM")
         *
         * DHKEM = Diffie-Hellman Key Encapsulation Mechanism
         *
         * - Uses Diffie-Hellman key agreement under the hood
         * - Combined with X25519 curve for the actual math
         * - Standardized in RFC 9180 (HPKE)
         */
        KEM kem = KEM.getInstance("DHKEM");

        /*
         * kem.newEncapsulator(publicKey)
         *
         * This only CONFIGURES the encapsulator. No key derivation happens here.
         *
         * - Stores the public key inside the encapsulator
         * - Prepares for encapsulation
         */
        KEM.Encapsulator encapsulator = kem.newEncapsulator(publicKey);

        /*
         * encapsulator.encapsulate()
         *
         * This ACTUALLY DERIVES the shared secret.
         *
         * Returns KEM.Encapsulated containing:
         *   - key()          : The derived shared secret (use this for AES encryption)
         *   - encapsulation(): Data to send to receiver (so they can derive the same secret)
         */
        KEM.Encapsulated encapsulated = encapsulator.encapsulate();

        System.out.println("KEM Algorithm: " + kem.getAlgorithm());
        System.out.println("Encapsulation Size: " + encapsulated.encapsulation().length + " bytes");
        System.out.println("Encapsulation (Base64): " +
                Base64.getEncoder().encodeToString(encapsulated.encapsulation()));
        System.out.println("Shared secret derived locally — NEVER transmitted.\n");

        return encapsulated;
    }

    // Step 3: Send only the encapsulation bytes to the receiver
    public static byte[] sendEncapsulation(KEM.Encapsulated encapsulated) {
        System.out.println("=== Step 3: Send Encapsulation ===");

        byte[] encapsulation = encapsulated.encapsulation();

        System.out.println("Encapsulation Size: " + encapsulation.length + " bytes");
        System.out.println("Note: Only the encapsulation is sent over the network.");
        System.out.println("Note: The shared secret is NEVER transmitted.\n");

        return encapsulation;
    }

    // Step 4: Receiver uses KEM + private key + encapsulation → derives same shared secret
    public static SecretKey decapsulate(PrivateKey privateKey, byte[] encapsulation)
            throws Exception {
        System.out.println("=== Step 4: Receiver Decapsulation ===");

        KEM kem = KEM.getInstance("DHKEM");

        /*
         * kem.newDecapsulator(privateKey)
         *
         * This only CONFIGURES the decapsulator. No key derivation happens here.
         */
        KEM.Decapsulator decapsulator = kem.newDecapsulator(privateKey);

        /*
         * decapsulator.decapsulate(encapsulation)
         *
         * This ACTUALLY DERIVES the shared secret.
         *
         * - Uses the private key + received encapsulation
         * - Mathematically derives the SAME shared secret as the sender
         */
        SecretKey sharedSecret = decapsulator.decapsulate(encapsulation);

        System.out.println("KEM Algorithm: " + kem.getAlgorithm());
        System.out.println("Shared secret derived locally — NEVER transmitted.\n");

        return sharedSecret;
    }

    // Step 5: Sender encrypts the message using AES-GCM
    public static EncryptionResult encryptMessage(SecretKey sharedSecret, String message)
            throws Exception {
        System.out.println("=== Step 5: Sender Encrypts Message (AES-GCM) ===");

        /*
         * Convert the KEM shared secret into an AES-256 key.
         * sharedSecret is 32 bytes — exactly what AES-256 needs.
         * SecretKeySpec wraps those bytes and marks them as an AES key.
         */
        SecretKey aesKey = new SecretKeySpec(sharedSecret.getEncoded(), "AES");

        /*
         * Generate a random 12-byte IV (Initialization Vector).
         * The IV ensures that encrypting the same message twice produces
         * different ciphertext each time — preventing pattern detection.
         * Never reuse the same IV with the same key.
         */
        byte[] iv = new byte[12];
        SecureRandom.getInstanceStrong().nextBytes(iv);

        /*
         * AES/GCM/NoPadding
         * GCM = Galois/Counter Mode
         * Provides both confidentiality (encryption) and integrity (authentication tag).
         * GCMParameterSpec: 128-bit authentication tag + IV
         */
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));

        /*
         * doFinal(plaintext)
         * Encrypts the message and appends the 128-bit authentication tag at the end.
         * Returns: encrypted bytes + authentication tag (combined in one byte array)
         */
        byte[] ciphertext = cipher.doFinal(message.getBytes());

        System.out.println("Original message : " + message);
        System.out.println("IV               : " + Base64.getEncoder().encodeToString(iv) + " (12 bytes)");
        System.out.println("Ciphertext       : " + Base64.getEncoder().encodeToString(ciphertext));
        System.out.println("Note: IV + ciphertext are sent to the receiver. AES key is NEVER sent.\n");

        return new EncryptionResult(iv, ciphertext);
    }

    // Step 6: Receiver decrypts the message using AES-GCM
    public static String decryptMessage(SecretKey receiverSharedSecret,
                                        byte[] iv,
                                        byte[] ciphertext) throws Exception {
        System.out.println("=== Step 6: Receiver Decrypts Message (AES-GCM) ===");

        /*
         * Receiver builds the same AES key from their own derived shared secret.
         * receiverSharedSecret == senderSharedSecret (mathematically identical).
         * So both sides produce the identical AES key — without ever transmitting it.
         */
        SecretKey aesKey = new SecretKeySpec(receiverSharedSecret.getEncoded(), "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));

        /*
         * doFinal(ciphertext)
         * The ciphertext byte array contains encrypted data + authentication tag at the end.
         * GCM internally:
         *   1. Extracts the authentication tag from the end of the ciphertext
         *   2. Recomputes the tag using the same key and IV
         *   3. If tags match  → decrypts and returns the original plaintext
         *   4. If tags differ → throws AEADBadTagException immediately (tampering detected)
         */
        byte[] plaintext = cipher.doFinal(ciphertext);
        String decryptedMessage = new String(plaintext);

        System.out.println("Decrypted message: " + decryptedMessage);
        System.out.println("Decryption successful.\n");

        return decryptedMessage;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║       KEM + AES DEMONSTRATION        ║");
        System.out.println("╚══════════════════════════════════════╝\n");

        // ── PHASE 1: KEM Key Exchange ──────────────────────────────────────────

        // Step 1: Receiver generates X25519 key pair
        KeyPair receiverKeyPair = generateKeyPair();

        // Step 2: Sender encapsulates — derives shared secret + encapsulation bytes
        KEM.Encapsulated encapsulated = encapsulate(receiverKeyPair.getPublic());
        SecretKey senderSharedSecret = encapsulated.key();

        // Step 3: Send only the encapsulation bytes to the receiver
        byte[] encapsulation = sendEncapsulation(encapsulated);

        // Step 4: Receiver decapsulates — derives the same shared secret independently
        SecretKey receiverSharedSecret = decapsulate(receiverKeyPair.getPrivate(), encapsulation);

        // Verify shared secrets match
        System.out.println("=== KEM Verification ===");
        boolean secretsMatch = Arrays.equals(
                senderSharedSecret.getEncoded(),
                receiverSharedSecret.getEncoded()
        );
        System.out.println("Shared Secrets Match: " + secretsMatch);
        System.out.println("Both sides derived the same secret — without transmitting it.\n");

        // ── PHASE 2: AES-GCM Encryption ───────────────────────────────────────

        String originalMessage = "Hello! This message is encrypted using KEM + AES-GCM.";

        // Step 5: Sender encrypts the message using the shared secret as AES key
        EncryptionResult result = encryptMessage(senderSharedSecret, originalMessage);

        // Step 6: Receiver decrypts the message using their derived shared secret
        String decryptedMessage = decryptMessage(receiverSharedSecret, result.iv(), result.ciphertext());

        // Final verification
        System.out.println("=== Final Verification ===");
        System.out.println("Original  : " + originalMessage);
        System.out.println("Decrypted : " + decryptedMessage);
        System.out.println("Match     : " + originalMessage.equals(decryptedMessage));
    }

    // Result container for IV + ciphertext
    public record EncryptionResult(byte[] iv, byte[] ciphertext) {}
}