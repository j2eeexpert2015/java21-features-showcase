package org.example.controller;

import org.example.dto.payment.*;
import org.example.dto.common.ApiResponse;
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

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
        logger.info("PaymentController initialized - Single POST mode");
    }

    // Main payment processing endpoint - SIMPLIFIED
    @PostMapping("/process")
    public ApiResponse processPayment(@RequestBody PaymentRequest request) {
        logger.info(">>> POST /api/payment/process - Payment processing request received");
        logger.info("Processing payment: method={}, amount=${}, customer={}, international={}",
                request.getPaymentMethod(), request.getAmount(),
                request.getCustomerType(), request.isInternational());

        try {
            Payment payment = request.toPayment();
            PaymentResponse response = paymentService.processPayment(
                    payment,
                    request.getCustomerType(),
                    request.isInternational()
            );

            ApiResponse apiResponse = createApiResponse(payment, request, response);
            logger.info("✓ Payment processing completed successfully. Transaction ID: {}",
                    response.getTransactionId());
            return apiResponse;

        } catch (Exception e) {
            logger.error("✗ Payment processing failed: {}", e.getMessage(), e);
            return new ApiResponse("PaymentController.processPayment",
                    "Payment processing error: " + e.getMessage())
                    .withError(e.getMessage());
        }
    }

    // Helper methods
    private ApiResponse createApiResponse(Payment payment, PaymentRequest request,
                                          PaymentResponse paymentResponse) {
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