package com.example.billing.service;

import com.example.billing.dto.PaymentRequest;
import com.example.billing.dto.PaymentResponse;
import com.example.billing.entity.Invoice;
import com.example.billing.entity.Payment;
import com.example.billing.exception.BusinessRuleException;
import com.example.billing.exception.ResourceNotFoundException;
import com.example.billing.repository.InvoiceRepository;
import com.example.billing.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final Clock clock;

    /** Records a payment and updates the invoice's amountPaid and status in one transaction. */
    public PaymentResponse record(Long invoiceId, PaymentRequest request) {
        String reference = request.referenceNo() != null && !request.referenceNo().isBlank()
                ? request.referenceNo().trim() : null;
        if (reference != null && paymentRepository.existsByReferenceNo(reference)) {
            throw new BusinessRuleException("A payment with reference " + reference + " is already recorded");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));

        invoiceService.applyPayment(invoice, request.amount());   // validates and updates status

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(request.amount());
        payment.setMethod(request.method());
        payment.setReferenceNo(reference);
        payment.setPaidAt(request.paidAt() != null ? request.paidAt() : Instant.now(clock));

        Payment saved = paymentRepository.save(payment);
        log.info("Payment {} of {} recorded on invoice {} (now {})",
                saved.getId(), saved.getAmount(), invoice.getInvoiceNumber(), invoice.getStatus());
        return PaymentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> forInvoice(Long invoiceId, Pageable pageable) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new ResourceNotFoundException("Invoice", invoiceId);
        }
        return paymentRepository.findByInvoiceId(invoiceId, pageable).map(PaymentResponse::from);
    }

    /** Payments received between two dates (inclusive), in the business time zone. */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> receivedBetween(LocalDate from, LocalDate to, Pageable pageable) {
        validateRange(from, to);
        return paymentRepository
                .findByPaidAtGreaterThanEqualAndPaidAtLessThan(startOf(from), startOf(to.plusDays(1)), pageable)
                .map(PaymentResponse::from);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalCollected(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return paymentRepository.sumCollectedBetween(startOf(from), startOf(to.plusDays(1)));
    }

    private Instant startOf(LocalDate date) {
        return date.atStartOfDay(clock.getZone()).toInstant();
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessRuleException("Both 'from' and 'to' dates are required");
        }
        if (to.isBefore(from)) {
            throw new BusinessRuleException("'to' date cannot be before 'from' date");
        }
    }
}
