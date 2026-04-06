package com.mywork.billing.service;

import com.mywork.billing.domain.Invoice;
import com.mywork.billing.domain.InvoiceStatus;
import com.mywork.billing.repository.InvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BillingReportService demonstrates core Java features in a realistic billing context.
 *
 * Covers: List, Map, Set, Streams, Lambdas, Method references,
 *         Optional, Collectors, Comparator, BigDecimal, Records
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class BillingReportService {

    private final InvoiceRepository invoiceRepository;

    public BillingReportService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Group invoices by status using Collectors.groupingBy.
     * Returns a Map<InvoiceStatus, List<Invoice>>
     *
     * Demonstrates: streams, groupingBy collector, Map
     */
    public Map<InvoiceStatus, List<Invoice>> groupInvoicesByStatus() {
        return invoiceRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(Invoice::getStatus)); // method reference
    }

    /**
     * Count invoices per status.
     * Returns a Map<InvoiceStatus, Long>
     *
     * Demonstrates: groupingBy with downstream counting collector
     */
    public Map<InvoiceStatus, Long> countInvoicesByStatus() {
        return invoiceRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(Invoice::getStatus, Collectors.counting()));
    }

    /**
     * Calculate total amount for a given status.
     *
     * Demonstrates: filter, map, reduce with BigDecimal, Optional
     */
    public BigDecimal totalAmountByStatus(InvoiceStatus status) {
        return invoiceRepository.findAll()
                .stream()
                .filter(invoice -> invoice.getStatus() == status)  // lambda predicate
                .map(Invoice::getAmount)                            // method reference
                .reduce(BigDecimal.ZERO, BigDecimal::add);          // method reference reduce
    }

    /**
     * Get the top N invoices by amount (highest first).
     *
     * Demonstrates: sorted with Comparator.reversed, limit, toList
     */
    public List<Invoice> getTopInvoicesByAmount(int limit) {
        return invoiceRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Invoice::getAmount).reversed())
                .limit(limit)
                .toList(); // Java 16+ unmodifiable list
    }

    /**
     * Get unique customer IDs that have overdue invoices.
     *
     * Demonstrates: filter, map, collect to Set (deduplication)
     */
    public Set<Long> getCustomerIdsWithOverdueInvoices() {
        return invoiceRepository.findAll()
                .stream()
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.OVERDUE)
                .map(invoice -> invoice.getCustomer().getId())
                .collect(Collectors.toSet()); // Set automatically deduplicates
    }

    /**
     * Find the invoice with the highest amount using Optional.
     *
     * Demonstrates: max with Comparator, Optional handling
     */
    public Optional<Invoice> findHighestValueInvoice() {
        return invoiceRepository.findAll()
                .stream()
                .max(Comparator.comparing(Invoice::getAmount));
    }

    /**
     * Calculate average invoice amount.
     *
     * Demonstrates: mapToDouble, OptionalDouble, BigDecimal precision
     */
    public BigDecimal calculateAverageInvoiceAmount() {
        List<Invoice> invoices = invoiceRepository.findAll();

        if (invoices.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = invoices.stream()
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // BigDecimal division requires explicit scale and rounding mode
        // Never use double for financial calculations - precision loss
        return total.divide(
                BigDecimal.valueOf(invoices.size()),
                2,                    // 2 decimal places
                RoundingMode.HALF_UP  // standard financial rounding
        );
    }

    /**
     * Get invoice numbers grouped by customer ID.
     * Returns Map<Long, List<String>>
     *
     * Demonstrates: groupingBy with mapping downstream collector
     */
    public Map<Long, List<String>> getInvoiceNumbersByCustomer() {
        return invoiceRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        invoice -> invoice.getCustomer().getId(),  // classifier
                        Collectors.mapping(Invoice::getInvoiceNumber, Collectors.toList()) // downstream
                ));
    }

    /**
     * Check if any invoice is overdue.
     *
     * Demonstrates: anyMatch short-circuit terminal operation
     */
    public boolean hasOverdueInvoices() {
        return invoiceRepository.findAll()
                .stream()
                .anyMatch(invoice -> invoice.getStatus() == InvoiceStatus.OVERDUE);
    }

    /**
     * Check if all invoices for a customer are paid.
     *
     * Demonstrates: filter + allMatch
     */
    public boolean areAllCustomerInvoicesPaid(Long customerId) {
        List<Invoice> customerInvoices = invoiceRepository.findAll()
                .stream()
                .filter(invoice -> invoice.getCustomer().getId().equals(customerId))
                .toList();

        if (customerInvoices.isEmpty()) {
            return true;
        }

        return customerInvoices.stream()
                .allMatch(invoice -> invoice.getStatus() == InvoiceStatus.PAID);
    }

    /**
     * Summary report using a Java record as the return type.
     *
     * Demonstrates: records as lightweight data carriers
     */
    public InvoiceSummary generateSummary() {
        List<Invoice> all = invoiceRepository.findAll();

        long total = all.size();
        long paid = all.stream().filter(i -> i.getStatus() == InvoiceStatus.PAID).count();
        long overdue = all.stream().filter(i -> i.getStatus() == InvoiceStatus.OVERDUE).count();
        long pending = all.stream().filter(i -> i.getStatus() == InvoiceStatus.PENDING).count();
        BigDecimal totalValue = all.stream()
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Generated invoice summary: total={}, paid={}, overdue={}, pending={}", total, paid, overdue, pending);

        return new InvoiceSummary(total, paid, overdue, pending, totalValue);
    }

    /**
     * Java record - immutable data carrier, compiler generates constructor,
     * getters, equals, hashCode, toString automatically.
     * Introduced in Java 16, good Java 21 talking point.
     */
    public record InvoiceSummary(
            long totalInvoices,
            long paidInvoices,
            long overdueInvoices,
            long pendingInvoices,
            BigDecimal totalValue
    ) {}
}
