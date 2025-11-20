package org.example.concepts.stringtemplate.scenarios;

/**
 * Shipment Alert Data
 * Immutable data holder for shipment notification
 */
public record ShipmentAlert(
        int orderId,
        String trackingNumber,
        String carrierName,
        double orderTotal
) {}