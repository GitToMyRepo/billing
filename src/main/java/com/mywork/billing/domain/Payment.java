package com.mywork.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString(exclude = "invoice") // exclude to avoid lazy loading issues
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotBlank
    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        if (paidAt == null) paidAt = LocalDateTime.now();
    }

    public Payment() {}

    public Payment(Invoice invoice, BigDecimal amount, String paymentMethod) {
        this.invoice = invoice;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }
}
