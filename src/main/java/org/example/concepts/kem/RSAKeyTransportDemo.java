package org.example.concepts.kem;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class RSAKeyTransportDemo {

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
        System.out.println("Symmetric Key (Base64): " +
                Base64.getEncoder().encodeToString(symmetricKey.getEncoded()));
        System.out.println("Symmetric key generated successfully.\n");

        return symmetricKey;
    }

    // Step 3: Encrypt Symmetric Key with Public Key (requires padding)
    public static byte[] encryptSymmetricKey(SecretKey symmetricKey, PublicKey publicKey)
            throws Exception {
        System.out.println("=== Step 3: Encrypt Symmetric Key with Public Key ===");

        // Using OAEP padding scheme
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.WRAP_MODE, publicKey);
        byte[] encryptedKey = cipher.wrap(symmetricKey);

        System.out.println("Cipher Algorithm: " + cipher.getAlgorithm());
        System.out.println("Padding Scheme: OAEPWithSHA-256AndMGF1Padding");
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
        System.out.println("Decrypted Key (Base64): " +
                Base64.getEncoder().encodeToString(decryptedKey.getEncoded()));
        System.out.println("Symmetric key decrypted successfully.\n");

        return decryptedKey;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("RSA KEY TRANSPORT DEMONSTRATION\n");

        // Step 1: Receiver generates key pair
        KeyPair receiverKeyPair = generateKeyPair();

        // Step 2: Sender generates random symmetric key
        SecretKey originalSymmetricKey = generateSymmetricKey();

        // Step 3: Sender encrypts symmetric key with receiver's public key
        byte[] encryptedKey = encryptSymmetricKey(
                originalSymmetricKey,
                receiverKeyPair.getPublic()
        );

        // Step 4: Receiver decrypts symmetric key with private key
        SecretKey recoveredSymmetricKey = decryptSymmetricKey(
                encryptedKey,
                receiverKeyPair.getPrivate()
        );

        // Verify keys match
        System.out.println("=== Verification ===");
        boolean keysMatch = Base64.getEncoder()
                .encodeToString(originalSymmetricKey.getEncoded())
                .equals(Base64.getEncoder()
                        .encodeToString(recoveredSymmetricKey.getEncoded()));
        System.out.println("Keys Match: " + keysMatch);
    }
}
