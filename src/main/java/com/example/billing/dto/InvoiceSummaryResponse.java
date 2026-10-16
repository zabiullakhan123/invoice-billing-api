package com.example.billing.dto;

import com.example.billing.entity.Invoice;
import com.example.billing.entity.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lightweight row for search results: no line items, so no N+1 queries. */
public record InvoiceSummaryResponse(
        Long id, String invoiceNumber, String customerName,
        LocalDate issueDate, LocalDate dueDate, InvoiceStatus status,
        BigDecimal grandTotal, BigDecimal balanceDue
) {
    public static InvoiceSummaryResponse from(Invoice inv) {
        return new InvoiceSummaryResponse(
                inv.getId(), inv.getInvoiceNumber(), inv.getCustomer().getName(),
                inv.getIssueDate(), inv.getDueDate(), inv.getStatus(),
                inv.getGrandTotal(), inv.getBalanceDue());
    }
}
