package com.mywork.billing.examples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Executable examples for Java interview Q&A topics.
 * Run these tests to see the concepts in action.
 * See JAVA_QA.md for explanations.
 */
class JavaConceptsExamplesTest {

    // -------------------------------------------------------------------------
    // String Pool & == vs equals
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("String Pool and == vs equals")
    class StringPoolExamples {

        @Test
        @DisplayName("String literals share the same object in the pool")
        void stringLiteralsSharePoolObject() {
            String s1 = "Hello"; // created in String pool
            String s2 = "Hello"; // reuses same pool object
            String s3 = new String("Hello"); // new object on heap, NOT in pool

            assertThat(s1 == s2).isTrue();   // same pool object
            assertThat(s1 == s3).isFalse();  // different objects
            assertThat(s1.equals(s3)).isTrue(); // same content

            // Always use equals() to compare String values, never ==
        }

        @Test
        @DisplayName("intern() forces a string into the pool")
        void internForcesStringIntoPool() {
            String s1 = new String("Hello"); // heap object
            String s2 = s1.intern();         // returns pool version
            String s3 = "Hello";             // pool object

            assertThat(s2 == s3).isTrue(); // both point to pool object
        }
    }

    // -------------------------------------------------------------------------
    // Collections - List, Set, Map
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Collections - List, Set, Map")
    class CollectionsExamples {

        @Test
        @DisplayName("List allows duplicates and maintains order")
        void listAllowsDuplicatesAndMaintainsOrder() {
            // Diamond operator <> - compiler infers type from left side
            // equivalent to new ArrayList<String>() but cleaner
            List<String> list = new ArrayList<>();
            list.add("invoice-1");
            list.add("invoice-2");
            list.add("invoice-1"); // duplicate allowed

            assertThat(list).hasSize(3);
            assertThat(list.get(0)).isEqualTo("invoice-1"); // access by index
        }

        @Test
        @DisplayName("Set does not allow duplicates")
        void setDoesNotAllowDuplicates() {
            Set<Long> customerIds = new HashSet<>();
            customerIds.add(1L);
            customerIds.add(2L);
            customerIds.add(1L); // duplicate - silently ignored

            assertThat(customerIds).hasSize(2); // only 2 unique values
        }

        @Test
        @DisplayName("Map stores key-value pairs with unique keys")
        void mapStoresKeyValuePairs() {
            Map<String, Integer> invoiceCounts = new HashMap<>();
            invoiceCounts.put("PAID", 5);
            invoiceCounts.put("PENDING", 3);
            invoiceCounts.put("PAID", 7); // overwrites existing key

            assertThat(invoiceCounts.get("PAID")).isEqualTo(7); // overwritten
            assertThat(invoiceCounts).hasSize(2);
        }
    }

    // -------------------------------------------------------------------------
    // Generics wildcards
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Generics - extends and super wildcards")
    class GenericsExamples {

        // Helper classes for wildcard examples
        static class Amount {}
        static class InvoiceAmount extends Amount {}
        static class PaymentAmount extends InvoiceAmount {}

        @Test
        @DisplayName("extends wildcard - read only, accepts T and subclasses")
        void extendsWildcardIsReadOnly() {
            List<InvoiceAmount> invoiceAmounts = new ArrayList<>();
            invoiceAmounts.add(new InvoiceAmount());

            // ? extends Amount - accepts List<Amount>, List<InvoiceAmount>, List<PaymentAmount>
            List<? extends Amount> readOnly = invoiceAmounts;

            Amount a = readOnly.get(0); // reading is fine - returns Amount
            // readOnly.add(new InvoiceAmount()); // COMPILE ERROR - can't add
            // readOnly.add(new Amount());         // COMPILE ERROR - can't add

            assertThat(a).isNotNull();
        }

        @Test
        @DisplayName("super wildcard - can add T and subclasses, reading returns Object")
        void superWildcardAllowsAdding() {
            List<Amount> amounts = new ArrayList<>();

            // ? super InvoiceAmount - accepts List<InvoiceAmount>, List<Amount>, List<Object>
            List<? super InvoiceAmount> writable = amounts;

            writable.add(new InvoiceAmount()); // OK - adding InvoiceAmount
            writable.add(new PaymentAmount()); // OK - adding subclass of InvoiceAmount

            Object o = writable.get(0); // reading returns Object only
            assertThat(o).isInstanceOf(InvoiceAmount.class);
        }

        // Helper method demonstrating PECS - Producer Extends, Consumer Super
        BigDecimal sumAmounts(List<? extends BigDecimal> amounts) {
            // extends - reading/producing values from the list
            BigDecimal total = BigDecimal.ZERO;
            for (BigDecimal amount : amounts) {
                total = total.add(amount); // reading each element is fine
            }
            return total;
        }

        void addAmounts(List<? super BigDecimal> target, List<BigDecimal> source) {
            // super - writing/consuming values into the list
            target.addAll(source);
        }
    }

    // -------------------------------------------------------------------------
    // Optional
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Optional")
    class OptionalExamples {

        @Test
        @DisplayName("orElseThrow throws when empty")
        void orElseThrowWhenEmpty() {
            Optional<String> empty = Optional.empty();

            assertThatThrownBy(() -> empty.orElseThrow(() -> new RuntimeException("not found")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("not found");
        }

        @Test
        @DisplayName("orElse returns default when empty")
        void orElseReturnsDefault() {
            Optional<BigDecimal> empty = Optional.empty();
            BigDecimal result = empty.orElse(BigDecimal.ZERO);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("map transforms value if present")
        void mapTransformsValue() {
            Optional<String> invoiceNumber = Optional.of("INV-001");
            Optional<Integer> length = invoiceNumber.map(String::length);
            assertThat(length).contains(7);
        }
    }

    // -------------------------------------------------------------------------
    // BigDecimal
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("BigDecimal - financial calculations")
    class BigDecimalExamples {

        @Test
        @DisplayName("double has precision loss - never use for money")
        void doubleHasPrecisionLoss() {
            double result = 0.1 + 0.2;
            assertThat(result).isNotEqualTo(0.3); // 0.30000000000000004!

            // BigDecimal is exact
            BigDecimal bdResult = new BigDecimal("0.1").add(new BigDecimal("0.2"));
            assertThat(bdResult).isEqualByComparingTo(new BigDecimal("0.3"));
        }

        @Test
        @DisplayName("BigDecimal from double still has precision issues")
        void bigDecimalFromDoubleHasPrecisionIssues() {
            BigDecimal bad = new BigDecimal(0.1);   // from double - imprecise
            BigDecimal good = new BigDecimal("0.1"); // from String - exact

            assertThat(bad).isNotEqualByComparingTo(good); // bad != 0.1 exactly
        }

        @Test
        @DisplayName("RoundingMode.HALF_UP - standard maths rounding")
        void halfUpRounding() {
            BigDecimal value = new BigDecimal("2.345");
            BigDecimal rounded = value.setScale(2, RoundingMode.HALF_UP);
            assertThat(rounded).isEqualByComparingTo(new BigDecimal("2.35"));
        }

        @Test
        @DisplayName("RoundingMode.HALF_EVEN - banker's rounding at exactly 0.5")
        void halfEvenRounding() {
            // digit before 5 is 4 (even) - rounds down
            assertThat(new BigDecimal("2.45").setScale(1, RoundingMode.HALF_EVEN))
                    .isEqualByComparingTo(new BigDecimal("2.4"));

            // digit before 5 is 3 (odd) - rounds up
            assertThat(new BigDecimal("2.35").setScale(1, RoundingMode.HALF_EVEN))
                    .isEqualByComparingTo(new BigDecimal("2.4"));
        }

        @Test
        @DisplayName("reduce is equivalent to a for loop sum")
        void reduceEquivalentToForLoop() {
            List<BigDecimal> amounts = List.of(
                    new BigDecimal("100.00"),
                    new BigDecimal("200.00"),
                    new BigDecimal("300.00")
            );

            // Stream reduce
            BigDecimal streamTotal = amounts.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Equivalent for loop
            BigDecimal loopTotal = BigDecimal.ZERO;
            for (BigDecimal amount : amounts) {
                loopTotal = loopTotal.add(amount);
            }

            assertThat(streamTotal).isEqualByComparingTo(loopTotal);
            assertThat(streamTotal).isEqualByComparingTo(new BigDecimal("600.00"));
        }
    }

    // -------------------------------------------------------------------------
    // Streams - map, filter, sorted, collect
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Streams - map, filter, sorted, collect")
    class StreamExamples {

        @Test
        @DisplayName("filter keeps elements matching a condition - same type in and out")
        void filterKeepsMatchingElements() {
            List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6);

            List<Integer> evens = numbers.stream()
                    .filter(n -> n % 2 == 0)
                    .toList();

            assertThat(evens).containsExactly(2, 4, 6);
        }

        @Test
        @DisplayName("map transforms each element - different type can come out")
        void mapTransformsElements() {
            List<String> names = List.of("John", "Jane", "Bob");

            // String -> Integer (length)
            List<Integer> lengths = names.stream()
                    .map(String::length)
                    .toList();
            assertThat(lengths).containsExactly(4, 4, 3);

            // String -> String (uppercase)
            List<String> upper = names.stream()
                    .map(String::toUpperCase)
                    .toList();
            assertThat(upper).containsExactly("JOHN", "JANE", "BOB");
        }

        @Test
        @DisplayName("map converts entity to DTO - most common Spring Boot use case")
        void mapEntityToDto() {
            record Person(String name, int age) {}
            record PersonDto(String name, int nameLength) {}

            List<Person> people = List.of(new Person("John", 30), new Person("Jane", 25));

            List<PersonDto> dtos = people.stream()
                    .map(p -> new PersonDto(p.name(), p.name().length()))
                    .toList();

            assertThat(dtos.get(0).name()).isEqualTo("John");
            assertThat(dtos.get(0).nameLength()).isEqualTo(4);
        }

        @Test
        @DisplayName("filter then map - chain operations")
        void filterThenMap() {
            List<String> names = List.of("John", "Jane", "Bob", "Al");

            List<Integer> lengths = names.stream()
                    .filter(n -> n.length() > 3)  // keep John, Jane
                    .map(String::length)
                    .toList();

            assertThat(lengths).containsExactly(4, 4);
        }

        @Test
        @DisplayName("sorted with Comparator")
        void sortedWithComparator() {
            List<Integer> numbers = List.of(3, 1, 4, 1, 5);

            List<Integer> ascending = numbers.stream().sorted().toList();
            assertThat(ascending).containsExactly(1, 1, 3, 4, 5);

            List<Integer> descending = numbers.stream()
                    .sorted(Comparator.reverseOrder())
                    .toList();
            assertThat(descending).containsExactly(5, 4, 3, 1, 1);
        }

        @Test
        @DisplayName("anyMatch and allMatch short-circuit")
        void anyMatchAndAllMatch() {
            List<Integer> numbers = List.of(2, 4, 6, 7, 8);

            assertThat(numbers.stream().anyMatch(n -> n % 2 != 0)).isTrue();
            assertThat(numbers.stream().allMatch(n -> n % 2 == 0)).isFalse();
            assertThat(numbers.stream().noneMatch(n -> n > 100)).isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // Stack vs Heap
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Stack vs Heap memory")
    class StackHeapExamples {

        @Test
        @DisplayName("primitives are copied by value, objects by reference")
        void primitivesVsObjectReferences() {
            int a = 5;
            int b = a;
            b = 10;
            assertThat(a).isEqualTo(5); // a unchanged - b is a separate copy

            List<String> list1 = new ArrayList<>();
            list1.add("hello");
            List<String> list2 = list1; // copy of reference - same heap object
            list2.add("world");
            assertThat(list1).hasSize(2); // list1 sees the change!
        }

        @Test
        @DisplayName("String pool is part of the heap")
        void stringPoolIsInHeap() {
            String s1 = "Hello";
            String s2 = new String("Hello");
            String s3 = "Hello";

            assertThat(s1 == s3).isTrue();
            assertThat(s1 == s2).isFalse();
            assertThat(s1.equals(s2)).isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // Static variables, memory leaks, Map keys, Collectors
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Static variables, memory leaks, Map keys, Collectors")
    class MemoryAndCollectorsExamples {

        // Simulated billing domain objects for examples
        enum InvoiceStatus { PENDING, PAID, OVERDUE }

        record Invoice(Long id, Long customerId, String invoiceNumber,
                       BigDecimal amount, InvoiceStatus status) {}

        private List<Invoice> sampleInvoices() {
            return List.of(
                    new Invoice(1L, 101L, "INV-001", new BigDecimal("100.00"), InvoiceStatus.PAID),
                    new Invoice(2L, 101L, "INV-002", new BigDecimal("200.00"), InvoiceStatus.PENDING),
                    new Invoice(3L, 102L, "INV-003", new BigDecimal("300.00"), InvoiceStatus.OVERDUE),
                    new Invoice(4L, 102L, "INV-004", new BigDecimal("150.00"), InvoiceStatus.PAID),
                    new Invoice(5L, 103L, "INV-005", new BigDecimal("250.00"), InvoiceStatus.PENDING)
            );
        }

        @Test
        @DisplayName("Mutable Map key causes lookup failure - always use immutable keys")
        void mutableMapKeyBreaksLookup() {
            // WRONG - mutable key
            List<String> mutableKey = new ArrayList<>();
            mutableKey.add("INV-001");
            Map<List<String>, BigDecimal> badMap = new HashMap<>();
            badMap.put(mutableKey, new BigDecimal("100.00"));

            mutableKey.add("INV-002"); // mutate key - hashCode changes!
            assertThat(badMap.get(mutableKey)).isNull(); // can't find it anymore!

            // CORRECT - immutable String key
            Map<String, BigDecimal> goodMap = new HashMap<>();
            goodMap.put("INV-001", new BigDecimal("100.00"));
            assertThat(goodMap.get("INV-001")).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Collectors.groupingBy - group invoices by status")
        void groupInvoicesByStatus() {
            List<Invoice> invoices = sampleInvoices();

            Map<InvoiceStatus, List<Invoice>> byStatus = invoices.stream()
                    .collect(java.util.stream.Collectors.groupingBy(Invoice::status));

            assertThat(byStatus.get(InvoiceStatus.PAID)).hasSize(2);
            assertThat(byStatus.get(InvoiceStatus.PENDING)).hasSize(2);
            assertThat(byStatus.get(InvoiceStatus.OVERDUE)).hasSize(1);
        }

        @Test
        @DisplayName("Collectors.counting - count invoices per status")
        void countInvoicesPerStatus() {
            List<Invoice> invoices = sampleInvoices();

            Map<InvoiceStatus, Long> counts = invoices.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            Invoice::status,
                            java.util.stream.Collectors.counting()
                    ));

            assertThat(counts.get(InvoiceStatus.PAID)).isEqualTo(2L);
            assertThat(counts.get(InvoiceStatus.PENDING)).isEqualTo(2L);
            assertThat(counts.get(InvoiceStatus.OVERDUE)).isEqualTo(1L);
        }

        @Test
        @DisplayName("Collectors.toMap - map invoice ID to amount")
        void mapInvoiceIdToAmount() {
            List<Invoice> invoices = sampleInvoices();

            Map<Long, BigDecimal> amountById = invoices.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Invoice::id,
                            Invoice::amount
                    ));

            assertThat(amountById.get(1L)).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(amountById.get(3L)).isEqualByComparingTo(new BigDecimal("300.00"));
        }

        @Test
        @DisplayName("Collectors.mapping - get invoice numbers per customer")
        void getInvoiceNumbersPerCustomer() {
            List<Invoice> invoices = sampleInvoices();

            Map<Long, List<String>> numbersByCustomer = invoices.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            Invoice::customerId,
                            java.util.stream.Collectors.mapping(
                                    Invoice::invoiceNumber,
                                    java.util.stream.Collectors.toList()
                            )
                    ));

            assertThat(numbersByCustomer.get(101L))
                    .containsExactlyInAnyOrder("INV-001", "INV-002");
            assertThat(numbersByCustomer.get(102L))
                    .containsExactlyInAnyOrder("INV-003", "INV-004");
        }

        @Test
        @DisplayName("Collectors.joining - build CSV of invoice numbers")
        void joinInvoiceNumbers() {
            List<Invoice> invoices = sampleInvoices();

            String csv = invoices.stream()
                    .map(Invoice::invoiceNumber)
                    .collect(java.util.stream.Collectors.joining(", "));

            assertThat(csv).isEqualTo("INV-001, INV-002, INV-003, INV-004, INV-005");
        }

        @Test
        @DisplayName("Collectors.toSet - unique customer IDs with overdue invoices")
        void uniqueCustomerIdsWithOverdueInvoices() {
            List<Invoice> invoices = sampleInvoices();

            Set<Long> overdueCustomerIds = invoices.stream()
                    .filter(i -> i.status() == InvoiceStatus.OVERDUE)
                    .map(Invoice::customerId)
                    .collect(java.util.stream.Collectors.toSet());

            assertThat(overdueCustomerIds).containsExactly(102L);
        }

        @Test
        @DisplayName("Total amount per customer using groupingBy and summingDouble")
        void totalAmountPerCustomer() {
            List<Invoice> invoices = sampleInvoices();

            // Group by customer, sum amounts
            Map<Long, BigDecimal> totalByCustomer = invoices.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            Invoice::customerId,
                            java.util.stream.Collectors.reducing(
                                    BigDecimal.ZERO,
                                    Invoice::amount,
                                    BigDecimal::add
                            )
                    ));

            // customer 101: 100 + 200 = 300
            assertThat(totalByCustomer.get(101L)).isEqualByComparingTo(new BigDecimal("300.00"));
            // customer 102: 300 + 150 = 450
            assertThat(totalByCustomer.get(102L)).isEqualByComparingTo(new BigDecimal("450.00"));
        }
    }
}
