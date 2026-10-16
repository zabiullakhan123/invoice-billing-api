package com.example.billing.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Pure money math for invoices, with no database access and no Spring dependencies,
 * so it can be unit-tested with plain JUnit.
 *
 * Per line:
 *   gross    = quantity x unitPrice
 *   discount = gross x discountPct / 100
 *   taxable  = gross - discount            (tax is applied AFTER the discount)
 *   tax      = taxable x taxRate / 100
 *   total    = taxable + tax
 *
 * Each amount is rounded to 2 decimal places (HALF_UP) per line, and the
 * invoice totals are the sum of the rounded line amounts. That way the totals
 * printed on the invoice always add up exactly.
 */
@Component
public class InvoiceCalculator {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public record LineAmounts(BigDecimal gross, BigDecimal discount, BigDecimal taxable,
                              BigDecimal tax, BigDecimal total) {}

    public record InvoiceTotals(BigDecimal subtotal, BigDecimal discountTotal,
                                BigDecimal taxTotal, BigDecimal grandTotal) {}

    /**
     * @param discountPct percentage 0-100 (e.g. 10 = 10%); null means no discount
     * @param taxRate     percentage 0-100 (e.g. 18 = 18% GST); null means no tax
     */
    public LineAmounts calculateLine(int quantity, BigDecimal unitPrice,
                                     BigDecimal discountPct, BigDecimal taxRate) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        BigDecimal discountRate = validPercent(discountPct, "Discount");
        BigDecimal tax = validPercent(taxRate, "Tax rate");

        BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(SCALE, ROUNDING);
        BigDecimal discount = percentOf(gross, discountRate);
        BigDecimal taxable = gross.subtract(discount);
        BigDecimal taxAmount = percentOf(taxable, tax);

        return new LineAmounts(gross, discount, taxable, taxAmount, taxable.add(taxAmount));
    }

    public InvoiceTotals total(List<LineAmounts> lines) {
        BigDecimal subtotal = zero(), discount = zero(), tax = zero(), grand = zero();
        for (LineAmounts l : lines) {
            subtotal = subtotal.add(l.gross());
            discount = discount.add(l.discount());
            tax = tax.add(l.tax());
            grand = grand.add(l.total());
        }
        return new InvoiceTotals(subtotal, discount, tax, grand);
    }

    private static BigDecimal percentOf(BigDecimal amount, BigDecimal percent) {
        return amount.multiply(percent).divide(HUNDRED, SCALE, ROUNDING);
    }

    private static BigDecimal validPercent(BigDecimal value, String name) {
        if (value == null) return BigDecimal.ZERO;
        if (value.signum() < 0 || value.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException(name + " must be between 0 and 100");
        }
        return value;
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE);
    }
}
