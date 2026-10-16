package com.example.billing.repository;

import com.example.billing.dto.InvoiceStatusSummary;
import com.example.billing.entity.Invoice;
import com.example.billing.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Search + pagination goes through findAll(Specification, Pageable) inherited from
 * JpaSpecificationExecutor, using InvoiceSpecifications.matching(criteria).
 */
public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    /** Detail view: invoice + customer + items in one query. */
    @EntityGraph(attributePaths = {"customer", "items"})
    Optional<Invoice> findWithDetailsById(Long id);

    /** Used before deactivating/deleting a customer. */
    boolean existsByCustomerId(Long customerId);

    /** Dashboard tiles: count and value per status. */
    @Query("""
           select i.status          as status,
                  count(i)          as invoiceCount,
                  sum(i.grandTotal) as totalAmount,
                  sum(i.grandTotal - i.amountPaid) as balanceDue
             from Invoice i
            group by i.status
           """)
    List<InvoiceStatusSummary> summarizeByStatus();

    /** Nightly overdue job (see InvoiceService.markOverdueInvoices). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update Invoice i
              set i.status = com.example.billing.entity.InvoiceStatus.OVERDUE
            where i.dueDate < :today
              and i.status in :statuses
           """)
    int markOverdue(@Param("today") LocalDate today,
                    @Param("statuses") Collection<InvoiceStatus> statuses);
}
