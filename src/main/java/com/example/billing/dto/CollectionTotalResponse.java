package com.example.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CollectionTotalResponse(LocalDate from, LocalDate to, BigDecimal totalCollected) {}
