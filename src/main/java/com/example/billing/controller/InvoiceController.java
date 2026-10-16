package com.example.billing.controller;

import com.example.billing.dto.*;
import com.example.billing.service.InvoiceService;
import com.example.billing.web.Pageables;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Set;

@Tag(name = "Invoices")
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private static final Set<String> INVOICE_SORTS = Set.of(
            "issueDate", "dueDate", "grandTotal", "invoiceNumber", "status", "createdAt", "customer.name");

    private final InvoiceService invoiceService;

    @Operation(summary = "Create a draft invoice; totals, tax and discounts are calculated server-side")
    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody CreateInvoiceRequest request,
                                                  @AuthenticationPrincipal UserDetails user) {
        InvoiceResponse created = invoiceService.create(request, user.getUsername());
        return ResponseEntity.created(URI.create("/api/invoices/" + created.id())).body(created);
    }

    @Operation(summary = "Search invoices with filters, paging and sorting")
    @GetMapping
    public PageResponse<InvoiceSummaryResponse> search(
            @ParameterObject InvoiceSearchCriteria criteria,
            @ParameterObject @PageableDefault(size = 20, sort = "issueDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return PageResponse.from(invoiceService.search(criteria,
                Pageables.requireAllowedSort(pageable, INVOICE_SORTS)));
    }

    @Operation(summary = "Invoice count and value per status (dashboard)")
    @GetMapping("/summary")
    public List<InvoiceStatusSummary> summary() {
        return invoiceService.statusSummary();
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable Long id) {
        return invoiceService.getById(id);
    }

    @Operation(summary = "Edit a draft invoice")
    @PutMapping("/{id}")
    public InvoiceResponse update(@PathVariable Long id, @Valid @RequestBody CreateInvoiceRequest request) {
        return invoiceService.update(id, request);
    }

    @Operation(summary = "Mark a draft as sent to the customer")
    @PostMapping("/{id}/send")
    public InvoiceResponse send(@PathVariable Long id) {
        return invoiceService.markAsSent(id);
    }

    @Operation(summary = "Cancel an invoice with no payments (ADMIN only)")
    @PostMapping("/{id}/cancel")
    public InvoiceResponse cancel(@PathVariable Long id) {
        return invoiceService.cancel(id);
    }

    @Operation(summary = "Delete a draft invoice")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        invoiceService.deleteDraft(id);
        return ResponseEntity.noContent().build();
    }
}
