package com.example.billing.dto;

import com.example.billing.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/** paidAt defaults to now when omitted. */
public record PaymentRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotNull PaymentMethod method,
        @Size(max = 100) String referenceNo,
        @PastOrPresent Instant paidAt
) {}
