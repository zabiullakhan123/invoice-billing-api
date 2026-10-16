package com.example.billing.repository;

import com.example.billing.dto.InvoiceSearchCriteria;
import com.example.billing.entity.Invoice;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class InvoiceSpecifications {

    private InvoiceSpecifications() {}

    /** Builds a WHERE clause from whichever filters were supplied; all of them are optional. */
    public static Specification<Invoice> matching(InvoiceSearchCriteria c) {
        return (root, query, cb) -> {
            // Fetch the customer for list rows, but not in the COUNT query Spring runs for paging
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("customer");
            }
            if (c == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();
            if (c.customerId() != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), c.customerId()));
            }
            if (c.status() != null) {
                predicates.add(cb.equal(root.get("status"), c.status()));
            }
            if (c.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("issueDate"), c.from()));
            }
            if (c.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("issueDate"), c.to()));
            }
            if (c.invoiceNumber() != null && !c.invoiceNumber().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("invoiceNumber")),
                        "%" + c.invoiceNumber().trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
