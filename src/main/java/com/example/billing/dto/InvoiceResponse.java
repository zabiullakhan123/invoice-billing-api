package com.example.billing.dto;

import com.example.billing.entity.Invoice;
import com.example.billing.entity.InvoiceItem;
import com.example.billing.entity.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        Long customerId,
        String customerName,
        LocalDate issueDate,
        LocalDate dueDate,
        InvoiceStatus status,
        List<Item> items,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal taxTotal,
        BigDecimal grandTotal,
        BigDecimal amountPaid,
        BigDecimal balanceDue,
        String notes
) {
    public record Item(Long id, String description, Integer quantity, BigDecimal unitPrice,
                       BigDecimal discountPct, BigDecimal discountAmount,
                       BigDecimal taxRate, BigDecimal taxAmount, BigDecimal lineTotal) {
        static Item from(InvoiceItem i) {
            return new Item(i.getId(), i.getDescription(), i.getQuantity(), i.getUnitPrice(),
                    i.getDiscountPct(), i.getDiscountAmount(),
                    i.getTaxRate(), i.getTaxAmount(), i.getLineTotal());
        }
    }

    public static InvoiceResponse from(Invoice inv) {
        return new InvoiceResponse(
                inv.getId(), inv.getInvoiceNumber(),
                inv.getCustomer().getId(), inv.getCustomer().getName(),
                inv.getIssueDate(), inv.getDueDate(), inv.getStatus(),
                inv.getItems().stream().map(Item::from).toList(),
                inv.getSubtotal(), inv.getDiscountTotal(), inv.getTaxTotal(), inv.getGrandTotal(),
                inv.getAmountPaid(), inv.getBalanceDue(), inv.getNotes());
    }
}
