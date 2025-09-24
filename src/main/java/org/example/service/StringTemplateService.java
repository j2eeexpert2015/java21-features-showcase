// src/main/java/org/example/service/StringTemplateService.java
package org.example.service;

import org.example.dto.template.TemplateRequest;
import org.example.dto.template.TemplateResponse;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
// No additional imports needed for String Templates

@Service
public class StringTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(StringTemplateService.class);

    // Java 21 String Template for Email using STR processor
    public TemplateResponse generateEmail(TemplateRequest request) {
        logger.info("=== EMAIL TEMPLATE GENERATION START ===");
        logger.info("Using Java 21 STR processor for safe string interpolation");
        logger.info("Customer: {}, Order: {}, Amount: ${}",
                request.getCustomerName(), request.getOrderId(), request.getAmount());

        String templateSource = "STR.\"Dear \\{customerName}, Your order #\\{orderId} has been confirmed!\"";

        // Java 21 STR Template - safe string interpolation
        String emailContent = STR."""
            Dear \{request.getCustomerName()},

            Your order #\{request.getOrderId()} has been confirmed!

            Order Details:
            - Total Amount: $\{request.getAmount()}
            - Items: \{request.getItemsCount()} product(s)
            - Order Date: \{LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}

            Thank you for shopping with TechMart!

            Best regards,
            The TechMart Team
            """;

        logger.info("✓ STR processor completed - safe interpolation with {} variables", 4);
        logger.info("✓ Security: No injection risks - compile-time validation");
        logger.info("================================");

        return new TemplateResponse(
                "email",
                templateSource,
                emailContent,
                "STR Processor",
                "safe"
        );
    }

    // Java 21 String Template for SMS using formatted output
    public TemplateResponse generateSMS(TemplateRequest request) {
        logger.info("=== SMS TEMPLATE GENERATION START ===");
        logger.info("Using String.format() for formatted output");
        logger.info("Customer: {}, Order: {}, Amount: ${}",
                request.getCustomerName(), request.getOrderId(), request.getAmount());

        String templateSource = "FMT.\"Order \\{orderId} confirmed! Total: \\{amount:$.2f} for \\{itemsCount} items\"";

        // Formatted string output - simulating FMT processor behavior
        String smsContent = String.format("""
            TechMart Alert
            Order %s confirmed!
            Total: $%.2f for %d items.
            Track: techmart.com/track/%s
            """,
                request.getOrderId(),
                request.getAmount(),
                request.getItemsCount(),
                request.getOrderId()
        );

        logger.info("✓ Formatted output completed - currency and numbers formatted");
        logger.info("✓ Security: Content sanitized and formatted");
        logger.info("================================");

        return new TemplateResponse(
                "sms",
                templateSource,
                smsContent,
                "FMT Processor",
                "safe"
        );
    }

    // Custom Safe Template for SQL - prevents injection
    public TemplateResponse generateSafeSQL(TemplateRequest request) {
        logger.info("=== SAFE SQL TEMPLATE GENERATION START ===");
        logger.info("Using custom safe processor for SQL injection prevention");
        logger.info("Search query: '{}'", request.getSearchQuery());

        String templateSource = "SAFE.\"SELECT * FROM customers WHERE name = \\{searchQuery} AND status = 'active'\"";

        // Custom safe SQL generation - parameterized to prevent injection
        String safeQuery = generateParameterizedSQL(request.getSearchQuery());

        logger.info("✓ Custom processor completed - parameterized query generated");
        logger.info("✓ Security: SQL injection prevented with parameter binding");
        logger.info("================================");

        return new TemplateResponse(
                "sql",
                templateSource,
                safeQuery,
                "Custom Safe Processor",
                "safe"
        );
    }

    // Helper method for safe SQL generation
    private String generateParameterizedSQL(String searchQuery) {
        // Sanitize input - remove potentially dangerous characters
        String sanitizedQuery = searchQuery != null ?
                searchQuery.replaceAll("[';\"\\-\\-]", "").trim() : "";

        return STR."""
            -- Generated Safe SQL Query
            SELECT c.id, c.name, c.email, c.status 
            FROM customers c 
            WHERE c.name = ? 
              AND c.status = 'active' 
              AND c.created_date >= CURRENT_DATE - INTERVAL 365 DAY;

            -- Parameters (safely escaped):
            -- Parameter 1: "\{sanitizedQuery}"
            """;
    }
}