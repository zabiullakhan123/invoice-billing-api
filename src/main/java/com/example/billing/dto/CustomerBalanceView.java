package com.example.billing.dto;

import java.math.BigDecimal;

/** Interface projection: Spring maps the query's select aliases to these getters. */
public interface CustomerBalanceView {
    Long getCustomerId();
    String getName();
    Long getOpenInvoices();
    BigDecimal getOutstanding();
}
