package com.mywork.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mywork.billing.domain.Customer;
import com.mywork.billing.domain.Invoice;
import com.mywork.billing.domain.InvoiceStatus;
import com.mywork.billing.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class BillingReportServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private BillingReportService billingReportService;

    private Customer customer;
    private Invoice paidInvoice;
    private Invoice pendingInvoice;
    private Invoice overdueInvoice;

    @BeforeEach
    void setUp() throws Exception {
        customer = new Customer("John Doe", "john@example.com", null);
        setId(customer, 1L);

        paidInvoice = new Invoice(customer, "INV-001", new BigDecimal("100.00"), LocalDate.now());
        paidInvoice.setStatus(InvoiceStatus.PAID);
        setId(paidInvoice, 1L);

        pendingInvoice = new Invoice(customer, "INV-002", new BigDecimal("200.00"), LocalDate.now());
        pendingInvoice.setStatus(InvoiceStatus.PENDING);
        setId(pendingInvoice, 2L);

        overdueInvoice = new Invoice(customer, "INV-003", new BigDecimal("300.00"), LocalDate.now());
        overdueInvoice.setStatus(InvoiceStatus.OVERDUE);
        setId(overdueInvoice, 3L);
    }

    // Helper to set id via reflection since JPA normally sets it
    private void setId(Object entity, Long id) throws Exception {
        var field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    @Test
    @DisplayName("groupInvoicesByStatus should group invoices by their status")
    void groupInvoicesByStatusShouldGroupCorrectly() {
        when(invoiceRepository.findAll()).thenReturn(List.of(paidInvoice, pendingInvoice, overdueInvoice));

        Map<InvoiceStatus, List<Invoice>> result = billingReportService.groupInvoicesByStatus();

        assertThat(result).hasSize(3);
        assertThat(result.get(InvoiceStatus.PAID)).containsExactly(paidInvoice);
        assertThat(result.get(InvoiceStatus.PENDING)).containsExactly(pendingInvoice);
        assertThat(result.get(InvoiceStatus.OVERDUE)).containsExactly(overdueInvoice);
    }

    @Test
    @DisplayName("countInvoicesByStatus should return count per status")
    void countInvoicesByStatusShouldReturnCorrectCounts() {
        when(invoiceRepository.findAll()).thenReturn(List.of(paidInvoice, pendingInvoice, overdueInvoice));

        Map<InvoiceStatus, Long> result = billingReportService.countInvoicesByStatus();

        assertThat(result.get(InvoiceStatus.PAID)).isEqualTo(1L);
        assertThat(result.get(InvoiceStatus.PENDING)).isEqualTo(1L);
        assertThat(result.get(InvoiceStatus.OVERDUE)).isEqualTo(1L);
    }

    @Test
    @DisplayName("totalAmountByStatus should sum amounts for given status")
    void totalAmountByStatusShouldSumCorrectly() {
        when(invoiceRepository.findAll()).thenReturn(List.of(paidInvoice, pendingInvoice, overdueInvoice));

        BigDecimal total = billingReportService.totalAmountByStatus(InvoiceStatus.PAID);

        assertThat(total).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("getTopInvoicesByAmount should return invoices sorted by amount descending")
    void getTopInvoicesByAmountShouldReturnSortedDescending() {
        when(invoiceRepository.findAll()).thenReturn(List.of(paidInvoice, pendingInvoice, overdueInvoice));

        List<Invoice> top2 = billingReportService.getTopInvoicesByAmount(2);

        assertThat(top2).hasSize(2);
        assertThat(top2.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(top2.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("getCustomerIdsWithOverdueInvoices should return unique customer IDs")
    void getCustomerIdsWithOverdueInvoicesShouldReturnUniqueIds() {
        when(invoiceRepository.findAll()).thenReturn(List.of(paidInvoice, overdueInvoice));

        Set<Long> result = billingReportService.getCustomerIdsWithOverdueInvoices();

        assertThat(result).containsExactly(1L);
    }

    @Test
    @DisplayName("findHighestValueInvoice should return invoice with max amount")
    void findHighestValueInvoiceShouldReturnMaxAmount() {
        when(invoiceRepository.findAll()).thenReturn(List.of(paidInvoice, pendingInvoice, overdueInvoice));

        Optional<Invoice> result = billingReportService.findHighestValueInvoice();

        assertThat(result).isPresent();
        assertThat(result.get().getAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    @DisplayName("findHighestValueInvoice should return empty Optional when no invoices")
    void findHighestValueInvoiceShouldReturnEmptyWhenNoInvoices() {
        when(invoiceRepository.findAll()).thenReturn(List.of());

        Optional<Invoice> result = billingReportService.findHighestValueInvoice();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("calculateAverageInvoiceAmount should return correct average")
    void calculateAverageInvoiceAmountShouldReturnCorrectAverage() {
        when(invoiceRepository.findAll()).thenReturn(List.of(paidInvoice, pendingInvoice, overdueInvoice));

        BigDecimal average = billingReportService.calculateAverageInvoiceAmount();

        // (100 + 200 + 300) / 3 = 200.00
        assertThat(average).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("hasOverdueInvoices should return true when overdue invoices exist")
    void hasOverdueInvoicesShouldReturnTrueWhenOverdueExists() {
        when(invoiceRepository.findAll()).thenReturn(List.of(paidInvoice, overdueInvoice));

        assertThat(billingReportService.hasOverdueInvoices()).isTrue();
    }

    @Test
    @DisplayName("hasOverdueInvoices should return false when no overdue invoices")
    void hasOverdueInvoicesShouldReturnFalseWhenNoneOverdue() {
        when(invoiceRepository.findAll()).thenReturn(List.of(paidInvoice, pendingInvoice));

        assertThat(billingReportService.hasOverdueInvoices()).isFalse();
    }

    @Test
    @DisplayName("areAllCustomerInvoicesPaid should return true when all paid")
    void areAllCustomerInvoicesPaidShouldReturnTrueWhenAllPaid() {
        when(invoiceRepository.findAll()).thenReturn(List.of(paidInvoice));

        assertThat(billingReportService.areAllCustomerInvoicesPaid(1L)).isTrue();
    }

    @Test
    @DisplayName("generateSummary should return correct summary record")
    void generateSummaryShouldReturnCorrectSummary() {
        when(invoiceRepository.findAll()).thenReturn(List.of(paidInvoice, pendingInvoice, overdueInvoice));

        BillingReportService.InvoiceSummary summary = billingReportService.generateSummary();

        assertThat(summary.totalInvoices()).isEqualTo(3);
        assertThat(summary.paidInvoices()).isEqualTo(1);
        assertThat(summary.overdueInvoices()).isEqualTo(1);
        assertThat(summary.pendingInvoices()).isEqualTo(1);
        assertThat(summary.totalValue()).isEqualByComparingTo(new BigDecimal("600.00"));
    }
}
