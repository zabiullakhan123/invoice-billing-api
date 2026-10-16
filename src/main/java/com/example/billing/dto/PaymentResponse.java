package com.example.billing.dto;

import com.example.billing.entity.Payment;
import com.example.billing.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(Long id, Long invoiceId, String invoiceNumber, BigDecimal amount,
                              PaymentMethod method, String referenceNo, Instant paidAt) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getInvoice().getId(), p.getInvoice().getInvoiceNumber(),
                p.getAmount(), p.getMethod(), p.getReferenceNo(), p.getPaidAt());
    }
}
