package com.example.billing.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record InvoiceItemRequest(
        @NotBlank @Size(max = 255) String description,
        @NotNull @Positive Integer quantity,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal discountPct,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxRate
) {}
