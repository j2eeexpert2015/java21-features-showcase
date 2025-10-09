// src/main/java/org/example/controller/PaymentController.java
package org.example.controller;

import org.example.dto.payment.*;
import org.example.dto.common.ApiResponse;
import org.example.model.payment.CustomerType;
import org.example.model.payment.Payment;
import org.example.service.PaymentService;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private PaymentRequest currentState; // Demo state for UI

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
        this.currentState = createDefaultState();
        logger.info("PaymentController initialized with default state: {} payment, ${}, {} customer",
                currentState.getPaymentMethod(), currentState.getAmount(), currentState.getCustomerType());
    }

    // Main payment processing endpoint
    @PostMapping("/process")
    public ApiResponse processPayment(@RequestBody(required = false) PaymentRequest request) {
        logger.info(" POST /api/payment/process - Payment processing request received");

        PaymentRequest activeRequest = (request != null) ? request : currentState;
        logger.info("Processing payment: method={}, amount=${}, customer={}, international={}",
                activeRequest.getPaymentMethod(), activeRequest.getAmount(),
                activeRequest.getCustomerType(), activeRequest.isInternational());

        try {
            Payment payment = activeRequest.toPayment();
            PaymentResponse response = paymentService.processPayment(
                    payment,
                    activeRequest.getCustomerType(),
                    activeRequest.isInternational()
            );

            ApiResponse apiResponse = createApiResponse(payment, activeRequest, response);
            logger.info("Payment processing completed successfully. Transaction ID: {}", response.getTransactionId());
            return apiResponse;

        } catch (Exception e) {
            logger.error("Payment processing failed: {}", e.getMessage(), e);
            return new ApiResponse("PaymentController.processPayment",
                    "Payment processing error: " + e.getMessage())
                    .withError(e.getMessage());
        }
    }

    // Update payment method - triggers pattern selection
    @PutMapping("/method/{method}")
    public ApiResponse selectPaymentMethod(@PathVariable String method) {
        logger.info(" PUT /api/payment/method/{} - Changing payment method", method);

        String previousMethod = currentState.getPaymentMethod();
        currentState.setPaymentMethod(method);

        String patternName = getPatternName(method);
        logger.info("Payment method updated: {} -> {} (Pattern: {})", previousMethod, method, patternName);

        return new ApiResponse("PaymentController.selectPaymentMethod",
                "Payment method changed to " + patternName)
                .withServiceCall("PaymentService.setPaymentType",
                        List.of("switch", patternName, "sealed"));
    }

    // Update customer type - affects guard conditions
    @PutMapping("/customer/type/{type}")
    public ApiResponse selectCustomerType(@PathVariable String type) {
        logger.info(" PUT /api/payment/customer/type/{} - Changing customer type", type);

        try {
            CustomerType previousType = currentState.getCustomerType();
            CustomerType customerType = CustomerType.valueOf(type.toUpperCase());
            currentState.setCustomerType(customerType);

            logger.info("Customer type updated: {} -> {} ({})",
                    previousType, customerType, customerType.getDisplayName());

            return new ApiResponse("PaymentController.setCustomerType",
                    "Customer type changed to " + customerType.getDisplayName())
                    .withServiceCall("CustomerService.updateType", List.of("pattern matching"));

        } catch (IllegalArgumentException e) {
            logger.error("Invalid customer type: {}", type);
            return new ApiResponse("PaymentController.setCustomerType",
                    "Invalid customer type: " + type)
                    .withError("Invalid customer type");
        }
    }

    // Toggle international status - activates international guard conditions
    @PutMapping("/international/{isInternational}")
    public ApiResponse toggleInternational(@PathVariable boolean isInternational) {
        logger.info(" PUT /api/payment/international/{} - Toggling international flag", isInternational);

        boolean previousStatus = currentState.isInternational();
        currentState.setInternational(isInternational);

        List<String> methods = new ArrayList<>();
        methods.add("when");
        if (isInternational) {
            methods.add("international");
        }

        logger.info("International status updated: {} -> {} (Guard conditions: {})",
                previousStatus, isInternational, isInternational ? "activated" : "standard");

        return new ApiResponse("PaymentController.toggleInternational",
                "International: " + (isInternational ? "enabled" : "disabled"))
                .withServiceCall("ValidationService.setInternational", methods);
    }

    // Update amount - triggers high-value guard conditions
    @PutMapping("/amount/{amount}")
    public ApiResponse setAmount(@PathVariable BigDecimal amount) {
        logger.info(" PUT /api/payment/amount/{} - Updating payment amount", amount);

        BigDecimal previousAmount = currentState.getAmount();
        currentState.setAmount(amount);

        List<String> methods = new ArrayList<>();
        methods.add("pattern matching");

        boolean triggersGuard = amount.compareTo(new BigDecimal("1000")) > 0;
        if (triggersGuard) {
            methods.add("when");
            methods.add("amount > 1000");
        }

        logger.info("Amount updated: ${} -> ${} (Guard conditions: {})",
                previousAmount, amount, triggersGuard ? "high-value triggered" : "standard");

        return new ApiResponse("PaymentController.setAmount",
                "Amount set to $" + amount)
                .withServiceCall("PaymentService.updateAmount", methods);
    }

    // Get current demo state for UI synchronization
    @GetMapping("/demo-state")
    public PaymentRequest getDemoState() {
        logger.info(" GET /api/payment/demo-state - Returning current demo state");
        logger.debug("Current state: method={}, amount=${}, customer={}, international={}",
                currentState.getPaymentMethod(), currentState.getAmount(),
                currentState.getCustomerType(), currentState.isInternational());
        return currentState;
    }

    // Reset demo to default state
    @PostMapping("/reset")
    public ApiResponse resetDemo() {
        logger.info(" POST /api/payment/reset - Resetting demo to default state");

        PaymentRequest previousState = currentState;
        this.currentState = createDefaultState();

        logger.info("Demo state reset: method={}->{}, amount=${}->${}, customer={}->{}",
                previousState.getPaymentMethod(), currentState.getPaymentMethod(),
                previousState.getAmount(), currentState.getAmount(),
                previousState.getCustomerType(), currentState.getCustomerType());

        return new ApiResponse("PaymentController.resetDemo",
                "Demo state reset to defaults")
                .withServiceCall("DemoService.initializeDefaults",
                        List.of("pattern matching", "demo reset"));
    }

    // Helper methods
    private PaymentRequest createDefaultState() {
        PaymentRequest request = new PaymentRequest();
        request.setCustomerId(1L);
        request.setPaymentMethod("credit");
        request.setAmount(new BigDecimal("500")); // Match UI default
        request.setCustomerType(CustomerType.VIP);
        request.setInternational(false);
        return request;
    }

    private ApiResponse createApiResponse(Payment payment, PaymentRequest request, PaymentResponse paymentResponse) {
        String paymentType = getPatternName(request.getPaymentMethod());
        List<String> methods = new ArrayList<>();

        methods.add("switch");
        methods.add(paymentType);
        methods.add("record pattern");

        boolean isHighValue = payment.getAmount().compareTo(new BigDecimal("1000")) > 0;
        if (isHighValue || request.isInternational()) {
            methods.add("when");
        }

        methods.add("sealed");

        logger.debug("API Response created: pattern={}, methods={}, status={}",
                paymentType, methods, paymentResponse.getStatus());

        return new ApiResponse("PaymentController.processPayment",
                "Payment processed using " + paymentType + " pattern")
                .withServiceCall("PaymentService.executePatternMatching", methods)
                .withMetadata("paymentResponse", paymentResponse);
    }

    private String getPatternName(String method) {
        return switch (method.toLowerCase()) {
            case "credit", "creditcard" -> "CreditCard";
            case "paypal" -> "PayPal";
            case "bank", "banktransfer" -> "BankTransfer";
            default -> {
                logger.warn("Unknown payment method: {}", method);
                yield "Unknown";
            }
        };
    }
}