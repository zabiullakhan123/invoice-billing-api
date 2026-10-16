package com.example.billing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 20) String phone,
        @Size(max = 500) String billingAddress,
        @Size(max = 20) String taxId
) {}
