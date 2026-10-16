package com.example.billing.repository;

import com.example.billing.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Payment history for one invoice. */
    Page<Payment> findByInvoiceId(Long invoiceId, Pageable pageable);

    /** Payments received in a period, with invoice + customer loaded for the list view. */
    @EntityGraph(attributePaths = {"invoice", "invoice.customer"})
    Page<Payment> findByPaidAtGreaterThanEqualAndPaidAtLessThan(Instant from, Instant to, Pageable pageable);

    /** Cash collected in a period (half-open range: from inclusive, to exclusive). */
    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.paidAt >= :from and p.paidAt < :to")
    BigDecimal sumCollectedBetween(@Param("from") Instant from, @Param("to") Instant to);

    /** Idempotency check: reject the same UPI/bank reference twice. */
    boolean existsByReferenceNo(String referenceNo);
}
