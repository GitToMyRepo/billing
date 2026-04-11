package com.mywork.billing.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

/**
 * Unit tests for PaymentGatewayService.
 * Circuit breaker behaviour is tested via integration tests with Resilience4j context.
 * Here we test the fallback method directly.
 */
@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

    private final PaymentGatewayService paymentGatewayService = new PaymentGatewayService();

    @Test
    @DisplayName("processPaymentShouldReturnSuccessResult")
    void processPaymentShouldReturnSuccessResult() {
        PaymentGatewayService.PaymentResult result = paymentGatewayService
                .processPayment(1L, new BigDecimal("100.00"), "CREDIT_CARD");

        assertThat(result.success()).isTrue();
        assertThat(result.message()).contains("successfully");
        assertThat(result.transactionId()).startsWith("TXN-");
    }

    @Test
    @DisplayName("fallbackProcessPaymentShouldReturnFailureResult")
    void fallbackProcessPaymentShouldReturnFailureResult() {
        RuntimeException simulatedGatewayError = new RuntimeException("Connection timeout");

        PaymentGatewayService.PaymentResult result = paymentGatewayService
                .fallbackProcessPayment(1L, new BigDecimal("100.00"), "CREDIT_CARD", simulatedGatewayError);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("temporarily unavailable");
        assertThat(result.transactionId()).isNull();
    }
}
