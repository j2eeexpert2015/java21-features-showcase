package org.example.concepts.kem;

import javax.crypto.KEM;
import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;

public class KEMDemo {

    public static void main(String[] args) throws Exception {

        // Receiver generates a key pair (one-time)
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("X25519");
        KeyPair receiverKeys = kpg.generateKeyPair();

        // Sender performs encapsulation
        KEM kem = KEM.getInstance("DHKEM");
        KEM.Encapsulator encapsulator =
                kem.newEncapsulator(receiverKeys.getPublic());

        KEM.Encapsulated encapsulated =
                encapsulator.encapsulate();

        SecretKey senderKey = encapsulated.key();
        byte[] encapsulation = encapsulated.encapsulation();

        // Receiver performs decapsulation
        KEM.Decapsulator decapsulator =
                kem.newDecapsulator(receiverKeys.getPrivate());

        SecretKey receiverKey =
                decapsulator.decapsulate(encapsulation);

        System.out.println("Keys match: " +
                Arrays.equals(
                        senderKey.getEncoded(),
                        receiverKey.getEncoded()
                ));
    }
}

