package com.example.billing.dto;

import com.example.billing.entity.InvoiceStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Bound from query params: ?customerId=&status=&from=&to=&invoiceNumber= */
public record InvoiceSearchCriteria(
        Long customerId,
        InvoiceStatus status,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        String invoiceNumber
) {}
