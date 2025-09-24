// src/main/java/org/example/controller/StringTemplateController.java
package org.example.controller;

import org.example.dto.template.TemplateRequest;
import org.example.dto.template.TemplateResponse;
import org.example.dto.common.ApiResponse;
import org.example.service.StringTemplateService;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class StringTemplateController {

    private static final Logger logger = LoggerFactory.getLogger(StringTemplateController.class);
    private final StringTemplateService templateService;
    private TemplateRequest currentState; // Demo state for UI

    public StringTemplateController(StringTemplateService templateService) {
        this.templateService = templateService;
        this.currentState = createDefaultState();
        logger.info("StringTemplateController initialized with default state: {}, ${}, {} items",
                currentState.getCustomerName(), currentState.getAmount(), currentState.getItemsCount());
    }

    // Generate email template using STR processor
    @PostMapping("/email")
    public ApiResponse generateEmail(@RequestBody(required = false) TemplateRequest request) {
        logger.info(">>> POST /api/templates/email - Email template generation request");

        TemplateRequest activeRequest = (request != null) ? request : currentState;

        try {
            TemplateResponse response = templateService.generateEmail(activeRequest);

            logger.info("Email template generated successfully using STR processor");

            return new ApiResponse("StringTemplateController.generateEmail",
                    "Email template generated using STR processor")
                    .withServiceCall("StringTemplateService.generateEmail",
                            List.of("STR", "Expression embedding", "Safety"))
                    .withMetadata("templateResponse", response);

        } catch (Exception e) {
            logger.error("Email template generation failed: {}", e.getMessage(), e);
            return new ApiResponse("StringTemplateController.generateEmail",
                    "Email template generation error: " + e.getMessage())
                    .withError(e.getMessage());
        }
    }

    // Generate SMS template using FMT processor
    @PostMapping("/sms")
    public ApiResponse generateSMS(@RequestBody(required = false) TemplateRequest request) {
        logger.info(">>> POST /api/templates/sms - SMS template generation request");

        TemplateRequest activeRequest = (request != null) ? request : currentState;

        try {
            TemplateResponse response = templateService.generateSMS(activeRequest);

            logger.info("SMS template generated successfully using FMT processor");

            return new ApiResponse("StringTemplateController.generateSMS",
                    "SMS template generated using FMT processor")
                    .withServiceCall("StringTemplateService.generateSMS",
                            List.of("FMT", "Expression embedding", "Formatted output"))
                    .withMetadata("templateResponse", response);

        } catch (Exception e) {
            logger.error("SMS template generation failed: {}", e.getMessage(), e);
            return new ApiResponse("StringTemplateController.generateSMS",
                    "SMS template generation error: " + e.getMessage())
                    .withError(e.getMessage());
        }
    }

    // Generate safe SQL template using custom processor
    @PostMapping("/sql")
    public ApiResponse generateSafeSQL(@RequestBody(required = false) TemplateRequest request) {
        logger.info(">>> POST /api/templates/sql - Safe SQL template generation request");

        TemplateRequest activeRequest = (request != null) ? request : currentState;

        try {
            TemplateResponse response = templateService.generateSafeSQL(activeRequest);

            logger.info("Safe SQL template generated successfully using custom processor");

            return new ApiResponse("StringTemplateController.generateSafeSQL",
                    "Safe SQL template generated using custom processor")
                    .withServiceCall("StringTemplateService.generateSafeSQL",
                            List.of("Custom", "Safety", "Injection prevention"))
                    .withMetadata("templateResponse", response);

        } catch (Exception e) {
            logger.error("Safe SQL template generation failed: {}", e.getMessage(), e);
            return new ApiResponse("StringTemplateController.generateSafeSQL",
                    "Safe SQL template generation error: " + e.getMessage())
                    .withError(e.getMessage());
        }
    }

    // Update demo data - customer name
    @PutMapping("/customer/{name}")
    public ApiResponse updateCustomerName(@PathVariable String name) {
        logger.info(">>> PUT /api/templates/customer/{} - Updating customer name", name);

        String previousName = currentState.getCustomerName();
        currentState.setCustomerName(name);

        logger.info("Customer name updated: '{}' -> '{}'", previousName, name);

        return new ApiResponse("StringTemplateController.updateCustomerName",
                "Customer name updated to " + name)
                .withServiceCall("TemplateStateService.updateCustomer", List.of("template variables"));
    }

    // Update demo data - order ID
    @PutMapping("/order/{orderId}")
    public ApiResponse updateOrderId(@PathVariable String orderId) {
        logger.info(">>> PUT /api/templates/order/{} - Updating order ID", orderId);

        String previousOrderId = currentState.getOrderId();
        currentState.setOrderId(orderId);

        logger.info("Order ID updated: '{}' -> '{}'", previousOrderId, orderId);

        return new ApiResponse("StringTemplateController.updateOrderId",
                "Order ID updated to " + orderId)
                .withServiceCall("TemplateStateService.updateOrder", List.of("template variables"));
    }

    // Update demo data - amount
    @PutMapping("/amount/{amount}")
    public ApiResponse updateAmount(@PathVariable BigDecimal amount) {
        logger.info(">>> PUT /api/templates/amount/{} - Updating amount", amount);

        BigDecimal previousAmount = currentState.getAmount();
        currentState.setAmount(amount);

        logger.info("Amount updated: ${} -> ${}", previousAmount, amount);

        return new ApiResponse("StringTemplateController.updateAmount",
                "Amount updated to $" + amount)
                .withServiceCall("TemplateStateService.updateAmount", List.of("template variables"));
    }

    // Update demo data - items count
    @PutMapping("/items/{count}")
    public ApiResponse updateItemsCount(@PathVariable Integer count) {
        logger.info(">>> PUT /api/templates/items/{} - Updating items count", count);

        Integer previousCount = currentState.getItemsCount();
        currentState.setItemsCount(count);

        logger.info("Items count updated: {} -> {}", previousCount, count);

        return new ApiResponse("StringTemplateController.updateItemsCount",
                "Items count updated to " + count)
                .withServiceCall("TemplateStateService.updateItems", List.of("template variables"));
    }

    // Update demo data - search query
    @PutMapping("/search/{query}")
    public ApiResponse updateSearchQuery(@PathVariable String query) {
        logger.info(">>> PUT /api/templates/search/{} - Updating search query", query);

        String previousQuery = currentState.getSearchQuery();
        currentState.setSearchQuery(query);

        logger.info("Search query updated: '{}' -> '{}'", previousQuery, query);

        return new ApiResponse("StringTemplateController.updateSearchQuery",
                "Search query updated to " + query)
                .withServiceCall("TemplateStateService.updateSearch", List.of("template variables"));
    }

    // Get current demo state for UI synchronization
    @GetMapping("/demo-state")
    public TemplateRequest getDemoState() {
        logger.info(">>> GET /api/templates/demo-state - Returning current demo state");
        logger.debug("Current state: name='{}', order='{}', amount=${}, items={}, search='{}'",
                currentState.getCustomerName(), currentState.getOrderId(),
                currentState.getAmount(), currentState.getItemsCount(), currentState.getSearchQuery());
        return currentState;
    }

    // Reset demo to default state
    @PostMapping("/reset")
    public ApiResponse resetDemo() {
        logger.info(">>> POST /api/templates/reset - Resetting demo to default state");

        TemplateRequest previousState = currentState;
        this.currentState = createDefaultState();

        logger.info("Demo state reset: name='{}'->'{}', amount=${}->${}, items={}->{}",
                previousState.getCustomerName(), currentState.getCustomerName(),
                previousState.getAmount(), currentState.getAmount(),
                previousState.getItemsCount(), currentState.getItemsCount());

        return new ApiResponse("StringTemplateController.resetDemo",
                "Demo state reset to defaults")
                .withServiceCall("DemoService.resetTemplateState",
                        List.of("template variables", "demo reset"));
    }

    // Helper method to create default demo state
    private TemplateRequest createDefaultState() {
        TemplateRequest request = new TemplateRequest();
        request.setCustomerName("John Doe");
        request.setOrderId("ORD-123456");
        request.setAmount(new BigDecimal("1299.99"));
        request.setItemsCount(3);
        request.setSearchQuery("John");
        return request;
    }
}