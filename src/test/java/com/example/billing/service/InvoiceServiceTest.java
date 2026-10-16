package com.example.billing.service;

import com.example.billing.dto.CreateInvoiceRequest;
import com.example.billing.dto.InvoiceItemRequest;
import com.example.billing.dto.InvoiceResponse;
import com.example.billing.entity.*;
import com.example.billing.exception.BusinessRuleException;
import com.example.billing.repository.CustomerRepository;
import com.example.billing.repository.InvoiceRepository;
import com.example.billing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests with mocked repositories and a fixed clock (today = 2026-10-15 in India). */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock CustomerRepository customerRepository;
    @Mock UserRepository userRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-15T06:30:00Z"), ZoneId.of("Asia/Kolkata"));
    private InvoiceService service;

    @BeforeEach
    void setUp() {
        service = new InvoiceService(invoiceRepository, customerRepository, userRepository,
                new InvoiceCalculator(), clock);
    }

    @Test
    void createCalculatesTotalsAndAssignsNumber() {
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer(7L, true)));
        when(userRepository.findByUsername("asha")).thenReturn(Optional.of(new User()));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(call -> {
            Invoice saved = call.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        var request = new CreateInvoiceRequest(7L, null, null, "Thanks for your business", List.of(
                new InvoiceItemRequest("Web hosting", 3, bd("199.99"), bd("10"), bd("18")),
                new InvoiceItemRequest("Support hours", 2, bd("500.00"), null, bd("5"))));

        InvoiceResponse response = service.create(request, "asha");

        assertThat(response.invoiceNumber()).isEqualTo("INV-2026-000042");
        assertThat(response.status()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(response.issueDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(response.dueDate()).isEqualTo(LocalDate.of(2026, 11, 14));   // default 30-day terms
        assertThat(response.subtotal()).isEqualByComparingTo("1599.97");
        assertThat(response.discountTotal()).isEqualByComparingTo("60.00");
        assertThat(response.taxTotal()).isEqualByComparingTo("147.19");
        assertThat(response.grandTotal()).isEqualByComparingTo("1687.16");
        assertThat(response.balanceDue()).isEqualByComparingTo("1687.16");
        assertThat(response.items()).hasSize(2);
    }

    @Test
    void createRejectsDeactivatedCustomer() {
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer(7L, false)));

        var request = new CreateInvoiceRequest(7L, null, null, null,
                List.of(new InvoiceItemRequest("Item", 1, bd("10"), null, null)));

        assertThatThrownBy(() -> service.create(request, "asha"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("deactivated");
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void updateIsOnlyAllowedForDrafts() {
        Invoice sent = invoice(InvoiceStatus.SENT, "1000.00", "0.00", LocalDate.of(2026, 11, 1));
        when(invoiceRepository.findWithDetailsById(1L)).thenReturn(Optional.of(sent));

        var request = new CreateInvoiceRequest(7L, null, null, null,
                List.of(new InvoiceItemRequest("Item", 1, bd("10"), null, null)));

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only draft invoices can be edited");
    }

    @Test
    void partialThenFullPaymentUpdatesStatus() {
        Invoice invoice = invoice(InvoiceStatus.SENT, "1000.00", "0.00", LocalDate.of(2026, 11, 1));

        service.applyPayment(invoice, bd("400.00"));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
        assertThat(invoice.getBalanceDue()).isEqualByComparingTo("600.00");

        service.applyPayment(invoice, bd("600.00"));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getBalanceDue()).isEqualByComparingTo("0.00");
    }

    @Test
    void overpaymentIsRejected() {
        Invoice invoice = invoice(InvoiceStatus.SENT, "1000.00", "0.00", LocalDate.of(2026, 11, 1));

        assertThatThrownBy(() -> service.applyPayment(invoice, bd("1000.01")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceeds balance due");
        assertThat(invoice.getAmountPaid()).isEqualByComparingTo("0.00");
    }

    @Test
    void paymentOnDraftIsRejected() {
        Invoice draft = invoice(InvoiceStatus.DRAFT, "1000.00", "0.00", LocalDate.of(2026, 11, 1));

        assertThatThrownBy(() -> service.applyPayment(draft, bd("100.00")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void unpaidInvoicePastDueBecomesOverdue() {
        Invoice invoice = invoice(InvoiceStatus.SENT, "1000.00", "0.00", LocalDate.of(2026, 10, 1));

        service.recalculatePaymentStatus(invoice);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
    }

    // ------------------------------------------------------------------ helpers

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static Customer customer(Long id, boolean active) {
        Customer c = new Customer();
        c.setId(id);
        c.setName("Acme Traders");
        c.setEmail("billing@acme.in");
        c.setActive(active);
        return c;
    }

    private static Invoice invoice(InvoiceStatus status, String grandTotal, String paid, LocalDate due) {
        Invoice i = new Invoice();
        i.setStatus(status);
        i.setGrandTotal(bd(grandTotal));
        i.setAmountPaid(bd(paid));
        i.setIssueDate(due.minusDays(30));
        i.setDueDate(due);
        i.setCustomer(customer(7L, true));
        return i;
    }
}
