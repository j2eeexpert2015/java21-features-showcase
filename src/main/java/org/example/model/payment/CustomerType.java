package org.example.model.payment;

public enum CustomerType {
    BASIC("Basic", "Standard"),
    PREMIUM("Premium", "High"),
    VIP("VIP", "Express");

    private final String displayName;
    private final String priority;

    CustomerType(String displayName, String priority) {
        this.displayName = displayName;
        this.priority = priority;
    }

    public String getDisplayName() { return displayName; }
    public String getPriority() { return priority; }
}
