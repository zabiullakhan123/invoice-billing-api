package com.example.billing.repository;

import com.example.billing.dto.CustomerBalanceView;
import com.example.billing.entity.Customer;
import com.example.billing.entity.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    // Derived queries: Spring generates the SQL from the method name
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);   // for updates

    /**
     * "Who owes us money?" Outstanding balance per customer, paged.
     * Sortable by the select aliases: name, outstanding, openInvoices.
     * A GROUP BY query needs its own countQuery for pagination.
     */
    @Query(value = """
            select c.id                                as customerId,
                   c.name                              as name,
                   count(i)                            as openInvoices,
                   coalesce(sum(i.grandTotal - i.amountPaid), 0) as outstanding
              from Invoice i
              join i.customer c
             where i.status in :statuses
             group by c.id, c.name
            """,
            countQuery = """
            select count(distinct i.customer.id)
              from Invoice i
             where i.status in :statuses
            """)
    Page<CustomerBalanceView> findOutstandingBalances(@Param("statuses") Collection<InvoiceStatus> statuses,
                                                      Pageable pageable);
}
