package com.example.billing.repository;

import com.example.billing.dto.CustomerSearchCriteria;
import com.example.billing.entity.Customer;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class CustomerSpecifications {

    private static final char ESCAPE = '\\';

    private CustomerSpecifications() {}

    /** ?q= matches name, email, phone or GSTIN (case-insensitive, contains). ?active= filters status. */
    public static Specification<Customer> matching(CustomerSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (c == null) {
                return cb.conjunction();
            }
            if (c.q() != null && !c.q().isBlank()) {
                String like = "%" + escapeLike(c.q().trim().toLowerCase()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like, ESCAPE),
                        cb.like(cb.lower(root.get("email")), like, ESCAPE),
                        cb.like(root.get("phone"), like, ESCAPE),
                        cb.like(cb.lower(root.get("taxId")), like, ESCAPE)
                ));
            }
            if (c.active() != null) {
                predicates.add(cb.equal(root.get("active"), c.active()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** Stops a search for "50%" or "a_b" from being treated as SQL wildcards. */
    static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
