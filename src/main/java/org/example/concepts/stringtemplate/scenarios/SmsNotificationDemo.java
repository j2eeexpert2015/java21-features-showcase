package org.example.concepts.stringtemplate.scenarios;

import static java.util.FormatProcessor.FMT;

/**
 * Use Case 4: SMS Alerts with FMT Processor
 * SMS notifications need precise number formatting for professional appearance.
 */
public class SmsNotificationDemo {

    public static void main(String[] args) {
        // Sample Data
        ShipmentAlert alert = new ShipmentAlert(
                45678,
                "1Z999AA10123456784",
                "FastShip",
                156.89
        );

        // String Template with FMT
        String sms = FMT."""
            Order #%06d\{alert.orderId()} shipped!
            Total: $%.2f\{alert.orderTotal()}

            Carrier: %s\{alert.carrierName()}
            Tracking: %s\{alert.trackingNumber()}

            Track: track.me/%s\{alert.trackingNumber().substring(0, 8)}
            """;

        // Output
        System.out.println(sms);

        System.out.println();
        System.out.println("Format specifiers explained:");
        System.out.println("%06d - Pads order number to 6 digits: 45678 → 045678");
        System.out.println("%.2f - Shows exactly 2 decimal places: $156.89 (not $156.9)");
        System.out.println("%s - Inserts text as-is");
        System.out.println();
        System.out.println("Why use FMT here:");
        System.out.println("Order IDs are consistently 6 digits (professional)");
        System.out.println("Prices always show cents (no $156.9 vs $156.89 inconsistency)");
    }
}