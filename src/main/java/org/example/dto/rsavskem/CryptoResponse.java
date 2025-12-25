package org.example.dto.rsavskem;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic response DTO for all cryptographic operations
 * Uses flexible Map structure to accommodate different operation results
 */
public class CryptoResponse {

    private boolean success;
    private String operation;
    private String message;
    private Map<String, Object> data;
    private String error;
    private LocalDateTime timestamp;

    public CryptoResponse() {
        this.data = new HashMap<>();
        this.timestamp = LocalDateTime.now();
    }

    public CryptoResponse(boolean success, String operation, String message) {
        this.success = success;
        this.operation = operation;
        this.message = message;
        this.data = new HashMap<>();
        this.timestamp = LocalDateTime.now();
    }

    // Builder-style method for adding data
    public CryptoResponse addData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    // Static factory methods for common scenarios

    public static CryptoResponse success(String operation, String message) {
        return new CryptoResponse(true, operation, message);
    }

    public static CryptoResponse error(String operation, String errorMessage) {
        CryptoResponse response = new CryptoResponse(false, operation, "Operation failed");
        response.setError(errorMessage);
        return response;
    }

    // Getters and Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

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

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "CryptoResponse{" +
                "success=" + success +
                ", operation='" + operation + '\'' +
                ", message='" + message + '\'' +
                ", dataKeys=" + data.keySet() +
                ", error='" + error + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}