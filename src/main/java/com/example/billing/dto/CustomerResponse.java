package com.example.billing.dto;

import com.example.billing.entity.Customer;

import java.time.Instant;

public record CustomerResponse(Long id, String name, String email, String phone,
                               String billingAddress, String taxId, boolean active, Instant createdAt) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.getId(), c.getName(), c.getEmail(), c.getPhone(),
                c.getBillingAddress(), c.getTaxId(), c.isActive(), c.getCreatedAt());
    }
}
