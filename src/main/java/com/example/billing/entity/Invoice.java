package com.example.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices", indexes = {
        @Index(name = "idx_invoice_customer", columnList = "customer_id"),
        @Index(name = "idx_invoice_status_due", columnList = "status, due_date")
})
@Getter
@Setter
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Assigned right after the first save (derived from id). */
    @Column(name = "invoice_number", unique = true, length = 30)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountTotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal taxTotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal grandTotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<InvoiceItem> items = new ArrayList<>();

    /** Optimistic locking: protects amountPaid from concurrent payment updates. */
    @Version
    private Long version;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }

    public void clearItems() {
        items.forEach(i -> i.setInvoice(null));
        items.clear();
    }

    @Transient
    public BigDecimal getBalanceDue() {
        return grandTotal.subtract(amountPaid);
    }
}
