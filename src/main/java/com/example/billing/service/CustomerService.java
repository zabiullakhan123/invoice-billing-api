package com.example.billing.service;

import com.example.billing.dto.CustomerBalanceView;
import com.example.billing.dto.CustomerRequest;
import com.example.billing.dto.CustomerResponse;
import com.example.billing.dto.CustomerSearchCriteria;
import com.example.billing.entity.Customer;
import com.example.billing.entity.InvoiceStatus;
import com.example.billing.exception.BusinessRuleException;
import com.example.billing.exception.ResourceNotFoundException;
import com.example.billing.repository.CustomerRepository;
import com.example.billing.repository.CustomerSpecifications;
import com.example.billing.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;

    public CustomerResponse create(CustomerRequest request) {
        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessRuleException("A customer with email " + request.email() + " already exists");
        }
        Customer customer = new Customer();
        apply(customer, request);
        return CustomerResponse.from(customerRepository.save(customer));
    }

    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = find(id);
        if (customerRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new BusinessRuleException("A customer with email " + request.email() + " already exists");
        }
        apply(customer, request);
        return CustomerResponse.from(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        return CustomerResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> search(CustomerSearchCriteria criteria, Pageable pageable) {
        return customerRepository
                .findAll(CustomerSpecifications.matching(criteria), pageable)
                .map(CustomerResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<CustomerBalanceView> outstandingBalances(Pageable pageable) {
        return customerRepository.findOutstandingBalances(
                EnumSet.of(InvoiceStatus.SENT, InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.OVERDUE),
                pageable);
    }

    /** Hides the customer from new invoices but keeps their billing history. */
    public CustomerResponse setActive(Long id, boolean active) {
        Customer customer = find(id);
        customer.setActive(active);
        return CustomerResponse.from(customer);
    }

    /** Hard delete is only allowed for customers that were never invoiced. */
    public void delete(Long id) {
        Customer customer = find(id);
        if (invoiceRepository.existsByCustomerId(id)) {
            throw new BusinessRuleException("Customer has invoices and cannot be deleted; deactivate it instead");
        }
        customerRepository.delete(customer);
    }

    private Customer find(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    private static void apply(Customer c, CustomerRequest r) {
        c.setName(r.name().trim());
        c.setEmail(r.email().trim().toLowerCase());
        c.setPhone(r.phone());
        c.setBillingAddress(r.billingAddress());
        c.setTaxId(r.taxId() != null ? r.taxId().trim().toUpperCase() : null);
    }
}
