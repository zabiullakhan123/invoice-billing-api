package com.example.billing.dto;

import com.example.billing.entity.InvoiceStatus;

import java.math.BigDecimal;

public interface InvoiceStatusSummary {
    InvoiceStatus getStatus();
    Long getInvoiceCount();
    BigDecimal getTotalAmount();
    BigDecimal getBalanceDue();
}
