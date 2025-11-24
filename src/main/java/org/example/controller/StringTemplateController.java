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
    private TemplateRequest currentState;

    public StringTemplateController(StringTemplateService templateService) {
        this.templateService = templateService;
        this.currentState = createDefaultState();
        logger.info("StringTemplateController initialized with default state: {}, ${}, {} items",
                currentState.getCustomerName(), currentState.getAmount(), currentState.getItemsCount());
    }

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
                            List.of("STR", "Expression embedding"))
                    .withMetadata("templateResponse", response);

        } catch (Exception e) {
            logger.error("Email template generation failed: {}", e.getMessage(), e);
            return new ApiResponse("StringTemplateController.generateEmail",
                    "Email template generation error: " + e.getMessage())
                    .withError(e.getMessage());
        }
    }

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

    @PostMapping("/sql")
    public ApiResponse generateSQL(@RequestBody(required = false) TemplateRequest request) {
        logger.info(">>> POST /api/templates/sql - SQL template generation request");

        TemplateRequest activeRequest = (request != null) ? request : currentState;

        try {
            TemplateResponse response = templateService.generateSafeSQL(activeRequest);
            logger.info("SQL template generated successfully using custom processor");

            return new ApiResponse("StringTemplateController.generateSQL",
                    "SQL template generated using custom processor")
                    .withServiceCall("StringTemplateService.generateSQL",
                            List.of("Custom", "Expression embedding"))
                    .withMetadata("templateResponse", response);

        } catch (Exception e) {
            logger.error("SQL template generation failed: {}", e.getMessage(), e);
            return new ApiResponse("StringTemplateController.generateSQL",
                    "SQL template generation error: " + e.getMessage())
                    .withError(e.getMessage());
        }
    }

    @GetMapping("/demo-state")
    public ApiResponse getDemoState() {
        logger.info(">>> GET /api/templates/demo-state - Returning current demo state");

        return new ApiResponse("StringTemplateController.getDemoState",
                "Current demo state retrieved")
                .withMetadata("demoState", currentState);
    }

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
                .withMetadata("demoState", currentState);
    }

    @PutMapping("/customer/{name}")
    public ApiResponse updateCustomerName(@PathVariable String name) {
        logger.info(">>> PUT /api/templates/customer/{} - Updating customer name", name);
        String previousName = currentState.getCustomerName();
        currentState.setCustomerName(name);
        logger.info("Customer name updated: '{}' -> '{}'", previousName, name);
        return new ApiResponse("StringTemplateController.updateCustomerName",
                "Customer name updated to " + name);
    }

    @PutMapping("/order/{orderId}")
    public ApiResponse updateOrderId(@PathVariable String orderId) {
        logger.info(">>> PUT /api/templates/order/{} - Updating order ID", orderId);
        String previousOrderId = currentState.getOrderId();
        currentState.setOrderId(orderId);
        logger.info("Order ID updated: '{}' -> '{}'", previousOrderId, orderId);
        return new ApiResponse("StringTemplateController.updateOrderId",
                "Order ID updated to " + orderId);
    }

    @PutMapping("/amount/{amount}")
    public ApiResponse updateAmount(@PathVariable BigDecimal amount) {
        logger.info(">>> PUT /api/templates/amount/{} - Updating amount", amount);
        BigDecimal previousAmount = currentState.getAmount();
        currentState.setAmount(amount);
        logger.info("Amount updated: ${} -> ${}", previousAmount, amount);
        return new ApiResponse("StringTemplateController.updateAmount",
                "Amount updated to $" + amount);
    }

    @PutMapping("/items/{count}")
    public ApiResponse updateItemsCount(@PathVariable Integer count) {
        logger.info(">>> PUT /api/templates/items/{} - Updating items count", count);
        Integer previousCount = currentState.getItemsCount();
        currentState.setItemsCount(count);
        logger.info("Items count updated: {} -> {}", previousCount, count);
        return new ApiResponse("StringTemplateController.updateItemsCount",
                "Items count updated to " + count);
    }

    @PutMapping("/search/{query}")
    public ApiResponse updateSearchQuery(@PathVariable String query) {
        logger.info(">>> PUT /api/templates/search/{} - Updating search query", query);
        String previousQuery = currentState.getSearchQuery();
        currentState.setSearchQuery(query);
        logger.info("Search query updated: '{}' -> '{}'", previousQuery, query);
        return new ApiResponse("StringTemplateController.updateSearchQuery",
                "Search query updated to " + query);
    }

    private TemplateRequest createDefaultState() {
        TemplateRequest request = new TemplateRequest();
        request.setCustomerName("Sarah Johnson");
        request.setOrderId("ORD-1001");
        request.setAmount(new BigDecimal("1299.99"));
        request.setItemsCount(3);
        request.setSearchQuery("sarah.johnson@example.com");
        return request;
    }
}