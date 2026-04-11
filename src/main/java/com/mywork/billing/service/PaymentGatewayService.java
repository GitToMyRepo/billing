package com.mywork.billing.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Simulates calling an external payment gateway (e.g. Stripe, PayPal).
 *
 * Demonstrates Circuit Breaker and Retry patterns using Resilience4j.
 *
 * Circuit Breaker states:
 * - CLOSED  — normal operation, requests pass through
 * - OPEN    — too many failures, requests immediately return fallback (no calls made)
 * - HALF_OPEN — after wait duration, allows a few test requests through to check recovery
 *
 * In a real billing system this would call an external payment API.
 * Here we simulate failures to demonstrate the pattern.
 */
@Slf4j
@Service
public class PaymentGatewayService {

    private static final String CIRCUIT_BREAKER_NAME = "paymentGateway";
    private static final String RETRY_NAME = "paymentGateway";

    /**
     * Process a payment through the external gateway.
     *
     * @CircuitBreaker - wraps the method with circuit breaker logic
     * @Retry - retries up to 3 times before giving up (configured in application.properties)
     *
     * If the circuit is OPEN, fallbackProcessPayment is called immediately
     * without attempting the real call.
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackProcessPayment")
    @Retry(name = RETRY_NAME)
    public PaymentResult processPayment(Long invoiceId, BigDecimal amount, String paymentMethod) {
        log.info("Processing payment for invoice {} amount {} via {}", invoiceId, amount, paymentMethod);

        // Simulate calling external payment gateway
        // In production: restTemplate.post("https://api.stripe.com/v1/charges", ...)
        simulateExternalCall();

        log.info("Payment processed successfully for invoice {}", invoiceId);
        return new PaymentResult(true, "Payment processed successfully", generateTransactionId());
    }

    /**
     * Fallback method - called when circuit is OPEN or all retries exhausted.
     * Must have the same signature as the original method plus a Throwable parameter.
     *
     * In a real system this might:
     * - Queue the payment for retry later (SQS)
     * - Return a pending status to the caller
     * - Alert the operations team
     */
    public PaymentResult fallbackProcessPayment(Long invoiceId, BigDecimal amount,
                                                 String paymentMethod, Throwable ex) {
        log.warn("Payment gateway unavailable for invoice {}. Reason: {}. Returning fallback.",
                invoiceId, ex.getMessage());
        return new PaymentResult(false, "Payment gateway temporarily unavailable. Please try again later.", null);
    }

    /**
     * Simulates an external HTTP call that may fail.
     * In production this would be a real REST call to a payment provider.
     */
    private void simulateExternalCall() {
        // Uncomment to simulate failures for testing circuit breaker:
        // throw new RuntimeException("Payment gateway timeout");
    }

    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis();
    }

    /**
     * Result record returned by the payment gateway.
     */
    public record PaymentResult(
            boolean success,
            String message,
            String transactionId
    ) {}
}
