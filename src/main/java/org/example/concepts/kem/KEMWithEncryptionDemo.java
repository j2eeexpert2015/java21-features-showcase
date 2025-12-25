package org.example.concepts.kem;

import javax.crypto.Cipher;
import javax.crypto.KEM;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class KEMWithEncryptionDemo {

    // ==================== KEY ESTABLISHMENT ====================

    // Step 1: Generate Key Pair
    public static KeyPair generateKeyPair() throws Exception {
        System.out.println("=== Step 1: Generate Key Pair ===");

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

        KEM kem = KEM.getInstance("DHKEM");
        KEM.Encapsulator encapsulator = kem.newEncapsulator(publicKey);
        KEM.Encapsulated encapsulated = encapsulator.encapsulate();

        System.out.println("KEM Algorithm: " + kem.getAlgorithm());
        System.out.println("Encapsulation Size: " + encapsulated.encapsulation().length + " bytes");
        System.out.println("Encapsulation (Base64): " +
                Base64.getEncoder().encodeToString(encapsulated.encapsulation()));
        System.out.println("Shared secret derived successfully.\n");

        return encapsulated;
    }

    // Step 3: Send only the encapsulation
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
        KEM.Decapsulator decapsulator = kem.newDecapsulator(privateKey);
        SecretKey sharedSecret = decapsulator.decapsulate(encapsulation);

        System.out.println("KEM Algorithm: " + kem.getAlgorithm());
        System.out.println("Shared secret derived successfully.\n");

        return sharedSecret;
    }

    // ==================== DATA ENCRYPTION ====================

    // Helper: Convert shared secret to AES key
    private static SecretKey toAesKey(SecretKey sharedSecret) {
        byte[] aesKeyBytes = Arrays.copyOf(sharedSecret.getEncoded(), 32);
        return new SecretKeySpec(aesKeyBytes, "AES");
    }

    // Step 5: Encrypt data using the shared secret
    public static EncryptedData encryptData(String plaintext, SecretKey sharedSecret)
            throws Exception {
        System.out.println("=== Step 5: Encrypt Data with Shared Secret ===");

        SecretKey aesKey = toAesKey(sharedSecret);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        byte[] iv = new byte[12];
        SecureRandom.getInstanceStrong().nextBytes(iv);

        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

        System.out.println("Plaintext: " + plaintext);
        System.out.println("IV (Base64): " + Base64.getEncoder().encodeToString(iv));
        System.out.println("Ciphertext (Base64): " + Base64.getEncoder().encodeToString(ciphertext));
        System.out.println("Data encrypted successfully.\n");

        return new EncryptedData(ciphertext, iv);
    }

    // Step 6: Decrypt data using the shared secret
    public static String decryptData(EncryptedData encryptedData, SecretKey sharedSecret)
            throws Exception {
        System.out.println("=== Step 6: Decrypt Data with Shared Secret ===");

        SecretKey aesKey = toAesKey(sharedSecret);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey,
                new GCMParameterSpec(128, encryptedData.iv));
        byte[] plaintext = cipher.doFinal(encryptedData.ciphertext);

        String decryptedText = new String(plaintext);
        System.out.println("Decrypted: " + decryptedText + "\n");

        return decryptedText;
    }

    // Helper class to hold encrypted data
    public static class EncryptedData {
        public final byte[] ciphertext;
        public final byte[] iv;

        public EncryptedData(byte[] ciphertext, byte[] iv) {
            this.ciphertext = ciphertext;
            this.iv = iv;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("KEM + AES ENCRYPTION DEMONSTRATION\n");

        String message = "Payment confirmed: $1,299.00 - Order #12345";

        // ==================== KEY ESTABLISHMENT ====================

        // Step 1: Receiver generates key pair
        KeyPair receiverKeyPair = generateKeyPair();

        // Step 2: Sender uses KEM + public key → derives shared secret + encapsulation
        KEM.Encapsulated encapsulated = encapsulate(receiverKeyPair.getPublic());
        SecretKey senderSharedSecret = encapsulated.key();

        // Step 3: Send only the encapsulation
        byte[] encapsulation = sendEncapsulation(encapsulated);

        // Step 4: Receiver uses KEM + private key + encapsulation → derives same shared secret
        SecretKey receiverSharedSecret = decapsulate(
                receiverKeyPair.getPrivate(),
                encapsulation
        );

        // ==================== DATA ENCRYPTION ====================

        // Step 5: Sender encrypts data with shared secret
        EncryptedData encryptedData = encryptData(message, senderSharedSecret);

        // Step 6: Receiver decrypts data with shared secret
        String decryptedMessage = decryptData(encryptedData, receiverSharedSecret);

        // ==================== VERIFICATION ====================
        System.out.println("=== Verification ===");
        System.out.println("Original:  " + message);
        System.out.println("Decrypted: " + decryptedMessage);
        System.out.println("Match: " + message.equals(decryptedMessage));
    }
}