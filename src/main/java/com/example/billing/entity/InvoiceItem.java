package com.example.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /** Percent, e.g. 10.00 = 10% */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPct;

    /** Percent, e.g. 18.00 = 18% GST */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    /** Stored so tax reports (e.g. GST by rate) don't need to recalculate. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;
}
