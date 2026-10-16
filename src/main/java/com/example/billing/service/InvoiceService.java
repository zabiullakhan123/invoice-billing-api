package com.example.billing.service;

import com.example.billing.dto.*;
import com.example.billing.entity.*;
import com.example.billing.exception.BusinessRuleException;
import com.example.billing.exception.ResourceNotFoundException;
import com.example.billing.repository.CustomerRepository;
import com.example.billing.repository.InvoiceRepository;
import com.example.billing.repository.InvoiceSpecifications;
import com.example.billing.repository.UserRepository;
import com.example.billing.service.InvoiceCalculator.InvoiceTotals;
import com.example.billing.service.InvoiceCalculator.LineAmounts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

    private static final int DEFAULT_PAYMENT_TERMS_DAYS = 30;
    private static final Set<InvoiceStatus> PAYABLE =
            EnumSet.of(InvoiceStatus.SENT, InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.OVERDUE);

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final InvoiceCalculator calculator;
    private final Clock clock;   // injected so tests can control "today"

    // ------------------------------------------------------------------ create / update

    public InvoiceResponse create(CreateInvoiceRequest request, String username) {
        Customer customer = findActiveCustomer(request.customerId());
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        invoice.setCreatedBy(creator);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setAmountPaid(BigDecimal.ZERO.setScale(2));
        applyHeader(invoice, request);
        applyItemsAndTotals(invoice, request.items());

        Invoice saved = invoiceRepository.save(invoice);
        // The number is derived from the generated id, so it is unique without a race condition
        saved.setInvoiceNumber("INV-%d-%06d".formatted(saved.getIssueDate().getYear(), saved.getId()));

        log.info("Invoice {} created for customer {} by {}", saved.getInvoiceNumber(), customer.getId(), username);
        return InvoiceResponse.from(saved);
    }

    /** Only drafts can be edited; once sent, an invoice is a legal document. */
    public InvoiceResponse update(Long id, CreateInvoiceRequest request) {
        Invoice invoice = findInvoice(id);
        requireStatus(invoice, InvoiceStatus.DRAFT, "Only draft invoices can be edited");

        if (!invoice.getCustomer().getId().equals(request.customerId())) {
            invoice.setCustomer(findActiveCustomer(request.customerId()));
        }
        applyHeader(invoice, request);
        applyItemsAndTotals(invoice, request.items());
        return InvoiceResponse.from(invoice);   // dirty checking saves on commit
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public InvoiceResponse getById(Long id) {
        return InvoiceResponse.from(findInvoice(id));
    }

    @Transactional(readOnly = true)
    public Page<InvoiceSummaryResponse> search(InvoiceSearchCriteria criteria, Pageable pageable) {
        return invoiceRepository
                .findAll(InvoiceSpecifications.matching(criteria), pageable)
                .map(InvoiceSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public List<InvoiceStatusSummary> statusSummary() {
        return invoiceRepository.summarizeByStatus();
    }

    // ------------------------------------------------------------------ lifecycle

    public InvoiceResponse markAsSent(Long id) {
        Invoice invoice = findInvoice(id);
        requireStatus(invoice, InvoiceStatus.DRAFT, "Only draft invoices can be sent");
        invoice.setStatus(InvoiceStatus.SENT);
        recalculatePaymentStatus(invoice);   // becomes OVERDUE immediately if the due date has passed
        return InvoiceResponse.from(invoice);
    }

    public InvoiceResponse cancel(Long id) {
        Invoice invoice = findInvoice(id);
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessRuleException("Invoice is already cancelled");
        }
        if (invoice.getAmountPaid().signum() > 0) {
            throw new BusinessRuleException("Cannot cancel an invoice that has payments; refund them first");
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        return InvoiceResponse.from(invoice);
    }

    public void deleteDraft(Long id) {
        Invoice invoice = findInvoice(id);
        requireStatus(invoice, InvoiceStatus.DRAFT, "Only draft invoices can be deleted; cancel it instead");
        invoiceRepository.delete(invoice);
    }

    // ------------------------------------------------------------------ payments

    /**
     * Called by PaymentService in the same transaction that saves the Payment row.
     * The @Version field on Invoice prevents two concurrent payments from both passing
     * the balance check.
     */
    public void applyPayment(Invoice invoice, BigDecimal amount) {
        if (!PAYABLE.contains(invoice.getStatus())) {
            throw new BusinessRuleException("Payments cannot be recorded on a " + invoice.getStatus() + " invoice");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Payment amount must be positive");
        }
        if (amount.compareTo(invoice.getBalanceDue()) > 0) {
            throw new BusinessRuleException("Payment of " + amount + " exceeds balance due of " + invoice.getBalanceDue());
        }
        invoice.setAmountPaid(invoice.getAmountPaid().add(amount));
        recalculatePaymentStatus(invoice);
    }

    /** Derives the status from the amount paid and the due date. Leaves DRAFT and CANCELLED alone. */
    public void recalculatePaymentStatus(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.DRAFT || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            return;
        }
        BigDecimal paid = invoice.getAmountPaid();
        boolean pastDue = invoice.getDueDate().isBefore(LocalDate.now(clock));

        InvoiceStatus next;
        if (paid.compareTo(invoice.getGrandTotal()) >= 0) {
            next = InvoiceStatus.PAID;
        } else if (pastDue) {
            next = InvoiceStatus.OVERDUE;
        } else if (paid.signum() > 0) {
            next = InvoiceStatus.PARTIALLY_PAID;
        } else {
            next = InvoiceStatus.SENT;
        }
        invoice.setStatus(next);
    }

    /** Nightly at 01:00: flag unpaid invoices whose due date has passed. Requires @EnableScheduling. */
    @Scheduled(cron = "0 0 1 * * *")
    public void markOverdueInvoices() {
        int updated = invoiceRepository.markOverdue(
                LocalDate.now(clock), EnumSet.of(InvoiceStatus.SENT, InvoiceStatus.PARTIALLY_PAID));
        if (updated > 0) {
            log.info("Marked {} invoice(s) as OVERDUE", updated);
        }
    }

    // ------------------------------------------------------------------ helpers

    private void applyHeader(Invoice invoice, CreateInvoiceRequest request) {
        LocalDate issueDate = request.issueDate() != null ? request.issueDate() : LocalDate.now(clock);
        LocalDate dueDate = request.dueDate() != null ? request.dueDate() : issueDate.plusDays(DEFAULT_PAYMENT_TERMS_DAYS);
        if (dueDate.isBefore(issueDate)) {
            throw new BusinessRuleException("Due date cannot be before issue date");
        }
        invoice.setIssueDate(issueDate);
        invoice.setDueDate(dueDate);
        invoice.setNotes(request.notes());
    }

    private void applyItemsAndTotals(Invoice invoice, List<InvoiceItemRequest> itemRequests) {
        invoice.clearItems();
        List<LineAmounts> lines = new ArrayList<>(itemRequests.size());

        for (InvoiceItemRequest r : itemRequests) {
            LineAmounts amounts;
            try {
                amounts = calculator.calculateLine(r.quantity(), r.unitPrice(), r.discountPct(), r.taxRate());
            } catch (IllegalArgumentException e) {
                throw new BusinessRuleException("Item '" + r.description() + "': " + e.getMessage());
            }

            InvoiceItem item = new InvoiceItem();
            item.setDescription(r.description());
            item.setQuantity(r.quantity());
            item.setUnitPrice(r.unitPrice());
            item.setDiscountPct(r.discountPct() != null ? r.discountPct() : BigDecimal.ZERO);
            item.setTaxRate(r.taxRate() != null ? r.taxRate() : BigDecimal.ZERO);
            item.setDiscountAmount(amounts.discount());
            item.setTaxAmount(amounts.tax());
            item.setLineTotal(amounts.total());
            invoice.addItem(item);

            lines.add(amounts);
        }

        InvoiceTotals totals = calculator.total(lines);
        invoice.setSubtotal(totals.subtotal());
        invoice.setDiscountTotal(totals.discountTotal());
        invoice.setTaxTotal(totals.taxTotal());
        invoice.setGrandTotal(totals.grandTotal());
    }

    private Invoice findInvoice(Long id) {
        return invoiceRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
    }

    private Customer findActiveCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        if (!customer.isActive()) {
            throw new BusinessRuleException("Customer " + id + " is deactivated and cannot be invoiced");
        }
        return customer;
    }

    private static void requireStatus(Invoice invoice, InvoiceStatus expected, String message) {
        if (invoice.getStatus() != expected) {
            throw new BusinessRuleException(message + " (current status: " + invoice.getStatus() + ")");
        }
    }
}
