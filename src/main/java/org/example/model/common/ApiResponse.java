package org.example.model.common;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class ApiResponse {
    private String controllerMethod;
    private Map<String, List<String>> serviceCalls = new HashMap<>();
    private String operationDescription;
    private Map<String, Object> metadata = new HashMap<>();
    private String error;

    // Constructors
    public ApiResponse() {}

    public ApiResponse(String controllerMethod, String operationDescription) {
        this.controllerMethod = controllerMethod;
        this.operationDescription = operationDescription;
    }

    // Builder pattern methods for fluent API
    public ApiResponse withServiceCall(String serviceMethod, List<String> java21Methods) {
        this.serviceCalls.put(serviceMethod, java21Methods);
        return this;
    }

    public ApiResponse withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    public ApiResponse withError(String error) {
        this.error = error;
        return this;
    }

    // Getters and setters
    public String getControllerMethod() { return controllerMethod; }
    public void setControllerMethod(String controllerMethod) { this.controllerMethod = controllerMethod; }

    public Map<String, List<String>> getServiceCalls() { return serviceCalls; }
    public void setServiceCalls(Map<String, List<String>> serviceCalls) { this.serviceCalls = serviceCalls; }

    public String getOperationDescription() { return operationDescription; }
    public void setOperationDescription(String operationDescription) { this.operationDescription = operationDescription; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
