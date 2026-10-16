package com.example.billing.dto;

/** Bound from query params: GET /api/customers?q=acme&active=true */
public record CustomerSearchCriteria(String q, Boolean active) {}
