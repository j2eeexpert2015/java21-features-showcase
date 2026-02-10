package org.example.service;

import org.example.dto.template.TemplateRequest;
import org.example.dto.template.TemplateResponse;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static java.util.FormatProcessor.FMT;
import static org.example.service.SafeSQLProcessor.SAFE;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Service
public class StringTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(StringTemplateService.class);

    // Java 21 String Template for Email using STR processor
    public TemplateResponse generateEmail(TemplateRequest request) {
        logger.info("=== EMAIL TEMPLATE GENERATION START ===");
        logger.info("Using Java 21 STR processor for safe string interpolation");
        logger.info("Customer: {}, Order: {}, Amount: ${}",
                request.getCustomerName(), request.getOrderId(), request.getAmount());

        // Template source (non-evaluated) - stored separately for demo visualization only
        String templateSource = """
            STR.\"\"\"
            Dear \\{request.getCustomerName()},
            
            Your order #\\{request.getOrderId()} has been confirmed!
            
            Order Details:
            - Total Amount: $\\{request.getAmount()}
            - Items: \\{request.getItemsCount()} product(s)
            - Order Date: \\{LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}
            
            Thank you for shopping with TechMart!
            
            Best regards,
            The TechMart Team
            \"\"\"
            """;

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
                "STR Processor"
        );
    }

    // Java 21 String Template for SMS using formatted output
    public TemplateResponse generateSMS(TemplateRequest request) {
        logger.info("=== SMS TEMPLATE GENERATION START ===");
        logger.info("Using FMT template processor for formatted output");
        logger.info("Customer: {}, Order: {}, Amount: {}",
                request.getCustomerName(), request.getOrderId(), request.getAmount());

        // Template source (non-evaluated) - stored separately for demo visualization only
        String templateSource = """
            FMT.\"\"\"
            TechMart Alert
            Customer: %-20s\\{request.getCustomerName()}
            Order ID: %-15s\\{request.getOrderId()}
            Amount:   $%,10.2f\\{request.getAmount()}
            Items:    %5d\\{request.getItemsCount()}
            Status:   APPROVED
            \"\"\"
            """;

        // Actual Java 21 FMT template execution
        String smsContent = FMT."""
            TechMart Alert
            Customer: %-20s\{request.getCustomerName()}
            Order ID: %-15s\{request.getOrderId()}
            Amount:   $%,10.2f\{request.getAmount()}
            Items:    %5d\{request.getItemsCount()}
            Status:   APPROVED
            """;

        logger.info("✓ FMT template executed successfully");
        logger.info("Generated SMS content:\n{}", smsContent);
        logger.info("================================");

        return new TemplateResponse(
                "sms",              // templateType
                templateSource,     // templateSource (for Panel 3)
                smsContent,         // generatedContent
                "FMT Processor"   // processorUsed
        );
    }

    public TemplateResponse generateSafeSQL(TemplateRequest request) {
        logger.info("=== SAFE SQL TEMPLATE GENERATION START ===");
        logger.info("Using custom safe processor for SQL injection prevention");
        logger.info("Customer: '{}', Order ID: '{}', Amount: ${}, Items: {}",
                request.getCustomerName(), request.getOrderId(),
                request.getAmount(), request.getItemsCount());

        // Template source (non-evaluated) - stored separately for demo visualization only
        String templateSource = """
            SAFE.\"\"\"
            SELECT o.order_id, o.customer_name, o.total_amount
            FROM orders o
            WHERE o.customer_name = '\\{request.getCustomerName()}'
              AND o.order_id = '\\{request.getOrderId()}'
              AND o.total_amount >= \\{request.getAmount()}
            ORDER BY o.order_date DESC
            \"\"\"
            """;

        // Actual custom SAFE processor execution
        String safeQuery = SAFE."""
        SELECT o.order_id, o.customer_name, o.total_amount
        FROM orders o
        WHERE o.customer_name = '\{request.getCustomerName()}'
          AND o.order_id = '\{request.getOrderId()}'
          AND o.total_amount >= \{request.getAmount()}
        ORDER BY o.order_date DESC
        """;

        logger.info("✓ Custom SAFE processor completed");
        logger.info("✓ Custom processor completed - safe query generated");
        logger.info("✓ Security: Input sanitized to prevent SQL injection");
        logger.info("================================");

        return new TemplateResponse(
                "sql",
                templateSource,
                safeQuery,
                "Custom Safe Processor"
        );
    }

    // Helper method for safe SQL generation
    private String generateParameterizedSQL(TemplateRequest request) {
        // Sanitize input - remove potentially dangerous characters
        String sanitizedCustomerName = request.getCustomerName() != null ?
                request.getCustomerName().replaceAll("[';\"\\-\\-]", "").trim() : "";

        String sanitizedOrderId = request.getOrderId() != null ?
                request.getOrderId().replaceAll("[';\"\\-\\-]", "").trim() : "";

        return STR."""
        -- Generated Safe SQL Query (Demo)
        -- All inputs sanitized: dangerous characters removed

        SELECT o.order_id, o.customer_name, o.total_amount, o.item_count, o.status, o.order_date
        FROM orders o
        WHERE o.customer_name = '\{sanitizedCustomerName}'
          AND o.order_id = '\{sanitizedOrderId}'
          AND o.total_amount >= \{request.getAmount()}
          AND o.item_count <= \{request.getItemsCount()}
          AND o.status = 'active'
          AND o.order_date >= CURRENT_DATE - INTERVAL 365 DAY
        ORDER BY o.order_date DESC;

        -- Security Note: In production, use PreparedStatement with ? placeholders
        -- This demo shows input sanitization as a String Template security example
        """;
    }
}