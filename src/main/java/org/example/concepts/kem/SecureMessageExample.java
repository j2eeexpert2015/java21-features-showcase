package org.example.concepts.kem;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Arrays;

public class SecureMessageExample {

    public static void main(String[] args) throws Exception {

        String message = "Payment confirmed: $1,299.00 - Order #12345";

        // ====== KEY ESTABLISHMENT (KEM) ======

        // Server generates key pair (one-time or long-lived)
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("X25519");
        KeyPair serverKeys = kpg.generateKeyPair();

        // Client encapsulates
        KEM kem = KEM.getInstance("DHKEM");
        KEM.Encapsulated encapsulated =
                kem.newEncapsulator(serverKeys.getPublic())
                        .encapsulate();

        // Derive AES key from shared secret
        byte[] aesKeyBytes =
                Arrays.copyOf(encapsulated.key().getEncoded(), 32);
        SecretKey aesKey =
                new SecretKeySpec(aesKeyBytes, "AES");

        // ====== ENCRYPTION (Client) ======

        byte[] iv = new byte[12];
        SecureRandom.getInstanceStrong().nextBytes(iv);

        Cipher encryptCipher =
                Cipher.getInstance("AES/GCM/NoPadding");
        encryptCipher.init(
                Cipher.ENCRYPT_MODE,
                aesKey,
                new GCMParameterSpec(128, iv)
        );

        byte[] ciphertext =
                encryptCipher.doFinal(message.getBytes());

        // ====== DECRYPTION (Server) ======

        SecretKey recoveredSecret =
                kem.newDecapsulator(serverKeys.getPrivate())
                        .decapsulate(encapsulated.encapsulation());

        SecretKey recoveredAesKey =
                new SecretKeySpec(
                        Arrays.copyOf(
                                recoveredSecret.getEncoded(), 32),
                        "AES"
                );

        Cipher decryptCipher =
                Cipher.getInstance("AES/GCM/NoPadding");
        decryptCipher.init(
                Cipher.DECRYPT_MODE,
                recoveredAesKey,
                new GCMParameterSpec(128, iv)
        );

        String decrypted =
                new String(decryptCipher.doFinal(ciphertext));

        System.out.println("Decrypted: " + decrypted);
    }
}

