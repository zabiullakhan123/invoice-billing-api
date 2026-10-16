package com.example.billing.web;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

public final class Pageables {

    private Pageables() {}

    /**
     * Rejects ?sort= values that aren't on the whitelist. Without this, ?sort=foo throws
     * PropertyReferenceException (a 500), and clients could sort on columns you never
     * meant to expose or that have no index.
     */
    public static Pageable requireAllowedSort(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot sort by '" + order.getProperty() + "'. Allowed: " + allowed);
            }
        }
        return pageable;
    }
}
