package org.example.dto.rsavskem;

/**
 * Generic request DTO for all cryptographic operations
 * Supports RSA Key Transport and KEM operations
 */
public class CryptoRequest {

    private String operation;  // e.g., "rsa-keygen", "kem-encapsulate", "encrypt-message"
    private String message;    // Message to encrypt/decrypt
    private String data;       // Generic field for passing any data (JSON string, Base64, etc.)

    public CryptoRequest() {
    }

    public CryptoRequest(String operation) {
        this.operation = operation;
    }

    public CryptoRequest(String operation, String message) {
        this.operation = operation;
        this.message = message;
    }

    public CryptoRequest(String operation, String message, String data) {
        this.operation = operation;
        this.message = message;
        this.data = data;
    }

    // Getters and Setters

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "CryptoRequest{" +
                "operation='" + operation + '\'' +
                ", message='" + message + '\'' +
                ", data='" + (data != null ? data.substring(0, Math.min(20, data.length())) + "..." : "null") + '\'' +
                '}';
    }
}
