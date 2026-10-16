package com.example.billing.controller;

import com.example.billing.dto.CollectionTotalResponse;
import com.example.billing.dto.PageResponse;
import com.example.billing.dto.PaymentRequest;
import com.example.billing.dto.PaymentResponse;
import com.example.billing.service.PaymentService;
import com.example.billing.web.Pageables;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

@Tag(name = "Payments")
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private static final Set<String> PAYMENT_SORTS = Set.of("paidAt", "amount");

    private final PaymentService paymentService;

    @Operation(summary = "Record a (partial or full) payment against an invoice")
    @PostMapping("/api/invoices/{invoiceId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse record(@PathVariable Long invoiceId, @Valid @RequestBody PaymentRequest request) {
        return paymentService.record(invoiceId, request);
    }

    @Operation(summary = "Payment history of one invoice")
    @GetMapping("/api/invoices/{invoiceId}/payments")
    public PageResponse<PaymentResponse> forInvoice(
            @PathVariable Long invoiceId,
            @ParameterObject @PageableDefault(size = 20, sort = "paidAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return PageResponse.from(paymentService.forInvoice(invoiceId,
                Pageables.requireAllowedSort(pageable, PAYMENT_SORTS)));
    }

    @Operation(summary = "Payments received in a date range, e.g. ?from=2026-10-01&to=2026-10-31")
    @GetMapping("/api/payments")
    public PageResponse<PaymentResponse> receivedBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @ParameterObject @PageableDefault(size = 20, sort = "paidAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return PageResponse.from(paymentService.receivedBetween(from, to,
                Pageables.requireAllowedSort(pageable, PAYMENT_SORTS)));
    }

    @Operation(summary = "Total cash collected in a date range")
    @GetMapping("/api/payments/total")
    public CollectionTotalResponse total(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return new CollectionTotalResponse(from, to, paymentService.totalCollected(from, to));
    }
}
