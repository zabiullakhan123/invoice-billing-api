package com.example.billing.controller;

import com.example.billing.dto.*;
import com.example.billing.service.CustomerService;
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
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Set;

@Tag(name = "Customers")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private static final Set<String> CUSTOMER_SORTS = Set.of("name", "email", "createdAt");
    private static final Set<String> BALANCE_SORTS = Set.of("name", "outstanding", "openInvoices");

    private final CustomerService customerService;

    @Operation(summary = "Create a customer")
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse created = customerService.create(request);
        return ResponseEntity.created(URI.create("/api/customers/" + created.id())).body(created);
    }

    @Operation(summary = "Search customers by name, email, phone or GSTIN")
    @GetMapping
    public PageResponse<CustomerResponse> search(
            @ParameterObject CustomerSearchCriteria criteria,
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return PageResponse.from(customerService.search(criteria,
                Pageables.requireAllowedSort(pageable, CUSTOMER_SORTS)));
    }

    @Operation(summary = "Outstanding balance per customer")
    @GetMapping("/balances")
    public PageResponse<CustomerBalanceView> outstandingBalances(
            @ParameterObject @PageableDefault(size = 20, sort = "outstanding", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return PageResponse.from(customerService.outstandingBalances(
                Pageables.requireAllowedSort(pageable, BALANCE_SORTS)));
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable Long id) {
        return customerService.getById(id);
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @Operation(summary = "Deactivate a customer (keeps billing history)")
    @PatchMapping("/{id}/deactivate")
    public CustomerResponse deactivate(@PathVariable Long id) {
        return customerService.setActive(id, false);
    }

    @PatchMapping("/{id}/activate")
    public CustomerResponse activate(@PathVariable Long id) {
        return customerService.setActive(id, true);
    }

    @Operation(summary = "Delete a customer that has no invoices (ADMIN only)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
