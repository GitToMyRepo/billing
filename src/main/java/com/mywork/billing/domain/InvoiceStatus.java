package com.mywork.billing.domain;

/**
 * Sealed interface + enum pattern — good Java 21 talking point.
 * Using a plain enum here; status transitions are enforced in the service layer.
 */
public enum InvoiceStatus {
    PENDING,
    PAID,
    OVERDUE,
    CANCELLED
}
