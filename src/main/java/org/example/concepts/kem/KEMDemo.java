package org.example.concepts.kem;

import javax.crypto.KEM;
import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.util.Base64;

public class KEMDemo {

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
        System.out.println("Shared Secret Algorithm: " + encapsulated.key().getAlgorithm());
        System.out.println("Shared Secret (Base64): " +
                Base64.getEncoder().encodeToString(encapsulated.key().getEncoded()));
        System.out.println("Encapsulation (Base64): " +
                Base64.getEncoder().encodeToString(encapsulated.encapsulation()));
        System.out.println("Encapsulation generated successfully.\n");

        return encapsulated;
    }

    // Step 3: Send only the encapsulation
    public static byte[] sendEncapsulation(KEM.Encapsulated encapsulated) {
        System.out.println("=== Step 3: Send Encapsulation ===");

        byte[] encapsulation = encapsulated.encapsulation();

        System.out.println("Encapsulation Size: " + encapsulation.length + " bytes");
        System.out.println("Encapsulation (Base64): " +
                Base64.getEncoder().encodeToString(encapsulation));
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
        System.out.println("Shared Secret Algorithm: " + sharedSecret.getAlgorithm());
        System.out.println("Shared Secret (Base64): " +
                Base64.getEncoder().encodeToString(sharedSecret.getEncoded()));
        System.out.println("Shared secret derived successfully.\n");

        return sharedSecret;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("KEM DEMONSTRATION\n");

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

        // Verify shared secrets match
        System.out.println("=== Verification ===");
        boolean secretsMatch = Base64.getEncoder()
                .encodeToString(senderSharedSecret.getEncoded())
                .equals(Base64.getEncoder()
                        .encodeToString(receiverSharedSecret.getEncoded()));
        System.out.println("Shared Secrets Match: " + secretsMatch);
    }
}
