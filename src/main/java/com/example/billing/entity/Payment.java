package com.example.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_invoice", columnList = "invoice_id"),
        @Index(name = "idx_payment_paid_at", columnList = "paid_at")
})
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    /** UPI transaction ID, cheque number, bank reference. Unique, so the same payment can't be recorded twice. */
    @Column(name = "reference_no", unique = true, length = 100)
    private String referenceNo;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;
}
