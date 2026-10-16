package com.example.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Used for both create and update. issueDate defaults to today, dueDate to issueDate + 30 days. */
public record CreateInvoiceRequest(
        @NotNull Long customerId,
        LocalDate issueDate,
        LocalDate dueDate,
        @Size(max = 1000) String notes,
        @NotEmpty @Size(max = 200) List<@Valid InvoiceItemRequest> items
) {}
