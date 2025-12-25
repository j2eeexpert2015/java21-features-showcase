package org.example.concepts.kem;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;

public class RSAKeyTransportWithEncryptionDemo {

    // ==================== KEY ESTABLISHMENT ====================

    // Step 1: Generate RSA Key Pair
    public static KeyPair generateKeyPair() throws Exception {
        System.out.println("=== Step 1: Generate RSA Key Pair ===");

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        System.out.println("Public Key Algorithm: " + keyPair.getPublic().getAlgorithm());
        System.out.println("Private Key Algorithm: " + keyPair.getPrivate().getAlgorithm());
        System.out.println("Key pair generated successfully.\n");

        return keyPair;
    }

    // Step 2: Randomly Generate Symmetric Key (AES)
    public static SecretKey generateSymmetricKey() throws Exception {
        System.out.println("=== Step 2: Randomly Generate Symmetric Key ===");

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey symmetricKey = keyGenerator.generateKey();

        System.out.println("Symmetric Key Algorithm: " + symmetricKey.getAlgorithm());
        System.out.println("Symmetric key generated successfully.\n");

        return symmetricKey;
    }

    // Step 3: Encrypt Symmetric Key with Public Key (requires padding)
    public static byte[] encryptSymmetricKey(SecretKey symmetricKey, PublicKey publicKey)
            throws Exception {
        System.out.println("=== Step 3: Encrypt Symmetric Key with Public Key ===");

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.WRAP_MODE, publicKey);
        byte[] encryptedKey = cipher.wrap(symmetricKey);

        System.out.println("Cipher Algorithm: " + cipher.getAlgorithm());
        System.out.println("Encrypted Key (Base64): " +
                Base64.getEncoder().encodeToString(encryptedKey));
        System.out.println("Symmetric key encrypted successfully.\n");

        return encryptedKey;
    }

    // Step 4: Decrypt Symmetric Key with Private Key
    public static SecretKey decryptSymmetricKey(byte[] encryptedKey, PrivateKey privateKey)
            throws Exception {
        System.out.println("=== Step 4: Decrypt Symmetric Key with Private Key ===");

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.UNWRAP_MODE, privateKey);
        SecretKey decryptedKey = (SecretKey) cipher.unwrap(encryptedKey, "AES", Cipher.SECRET_KEY);

        System.out.println("Decrypted Key Algorithm: " + decryptedKey.getAlgorithm());
        System.out.println("Symmetric key decrypted successfully.\n");

        return decryptedKey;
    }

    // ==================== DATA ENCRYPTION ====================

    // Step 5: Encrypt data using the symmetric key
    public static EncryptedData encryptData(String plaintext, SecretKey symmetricKey)
            throws Exception {
        System.out.println("=== Step 5: Encrypt Data with Symmetric Key ===");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        byte[] iv = new byte[12];
        SecureRandom.getInstanceStrong().nextBytes(iv);

        cipher.init(Cipher.ENCRYPT_MODE, symmetricKey, new GCMParameterSpec(128, iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

        System.out.println("Plaintext: " + plaintext);
        System.out.println("IV (Base64): " + Base64.getEncoder().encodeToString(iv));
        System.out.println("Ciphertext (Base64): " + Base64.getEncoder().encodeToString(ciphertext));
        System.out.println("Data encrypted successfully.\n");

        return new EncryptedData(ciphertext, iv);
    }

    // Step 6: Decrypt data using the symmetric key
    public static String decryptData(EncryptedData encryptedData, SecretKey symmetricKey)
            throws Exception {
        System.out.println("=== Step 6: Decrypt Data with Symmetric Key ===");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, symmetricKey,
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
        System.out.println("RSA KEY TRANSPORT + AES ENCRYPTION DEMONSTRATION\n");

        String message = "Payment confirmed: $1,299.00 - Order #12345";

        // ==================== KEY ESTABLISHMENT ====================

        // Step 1: Receiver generates key pair
        KeyPair receiverKeyPair = generateKeyPair();

        // Step 2: Sender generates random symmetric key
        SecretKey senderSymmetricKey = generateSymmetricKey();

        // Step 3: Sender encrypts symmetric key with receiver's public key
        byte[] encryptedKey = encryptSymmetricKey(
                senderSymmetricKey,
                receiverKeyPair.getPublic()
        );

        // Step 4: Receiver decrypts symmetric key with private key
        SecretKey receiverSymmetricKey = decryptSymmetricKey(
                encryptedKey,
                receiverKeyPair.getPrivate()
        );

        // ==================== DATA ENCRYPTION ====================

        // Step 5: Sender encrypts data with symmetric key
        EncryptedData encryptedData = encryptData(message, senderSymmetricKey);

        // Step 6: Receiver decrypts data with symmetric key
        String decryptedMessage = decryptData(encryptedData, receiverSymmetricKey);

        // ==================== VERIFICATION ====================
        System.out.println("=== Verification ===");
        System.out.println("Original:  " + message);
        System.out.println("Decrypted: " + decryptedMessage);
        System.out.println("Match: " + message.equals(decryptedMessage));
    }
}