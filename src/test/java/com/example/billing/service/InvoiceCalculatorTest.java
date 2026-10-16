package com.example.billing.service;

import com.example.billing.service.InvoiceCalculator.InvoiceTotals;
import com.example.billing.service.InvoiceCalculator.LineAmounts;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceCalculatorTest {

    private final InvoiceCalculator calculator = new InvoiceCalculator();

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    @Test
    void appliesDiscountBeforeTax() {
        // 3 x 199.99 = 599.97; 10% off = 60.00 (59.997 rounded); taxable 539.97; 18% = 97.19
        LineAmounts line = calculator.calculateLine(3, bd("199.99"), bd("10"), bd("18"));

        assertThat(line.gross()).isEqualByComparingTo("599.97");
        assertThat(line.discount()).isEqualByComparingTo("60.00");
        assertThat(line.taxable()).isEqualByComparingTo("539.97");
        assertThat(line.tax()).isEqualByComparingTo("97.19");
        assertThat(line.total()).isEqualByComparingTo("637.16");
    }

    @Test
    void nullDiscountAndTaxAreTreatedAsZero() {
        LineAmounts line = calculator.calculateLine(2, bd("500.00"), null, null);

        assertThat(line.discount()).isEqualByComparingTo("0");
        assertThat(line.tax()).isEqualByComparingTo("0");
        assertThat(line.total()).isEqualByComparingTo("1000.00");
    }

    @Test
    void roundsHalfUp() {
        // 0.05 x 50% = 0.025 -> 0.03
        LineAmounts line = calculator.calculateLine(1, bd("0.05"), null, bd("50"));
        assertThat(line.tax()).isEqualByComparingTo("0.03");
    }

    @Test
    void invoiceTotalsAreSumOfRoundedLines() {
        LineAmounts a = calculator.calculateLine(3, bd("199.99"), bd("10"), bd("18"));
        LineAmounts b = calculator.calculateLine(2, bd("500.00"), null, bd("5"));

        InvoiceTotals totals = calculator.total(List.of(a, b));

        assertThat(totals.subtotal()).isEqualByComparingTo("1599.97");
        assertThat(totals.discountTotal()).isEqualByComparingTo("60.00");
        assertThat(totals.taxTotal()).isEqualByComparingTo("147.19");
        assertThat(totals.grandTotal()).isEqualByComparingTo("1687.16");
        // The printed invoice must reconcile exactly
        assertThat(totals.subtotal().subtract(totals.discountTotal()).add(totals.taxTotal()))
                .isEqualByComparingTo(totals.grandTotal());
    }

    @Test
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> calculator.calculateLine(0, bd("10"), null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculateLine(1, bd("-1"), null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculateLine(1, bd("10"), bd("101"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
