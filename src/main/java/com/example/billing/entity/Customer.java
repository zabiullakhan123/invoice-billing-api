package com.example.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customer_name", columnList = "name"),
        @Index(name = "idx_customer_tax_id", columnList = "tax_id")
})
@Getter
@Setter
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String billingAddress;

    /** GSTIN / VAT number */
    @Column(name = "tax_id", length = 20)
    private String taxId;

    /** Soft delete: customers with invoices are deactivated, never removed. */
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    private Instant createdAt;
}
