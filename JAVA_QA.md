# Core Java Q&A

Java-specific questions and answers for interview preparation.
For Spring Boot, CI/CD, and testing topics see [INTERVIEW_QA.md](INTERVIEW_QA.md).

---

**Q: What is the difference between List, Set and Map?**

| | List | Set | Map |
|---|---|---|---|
| Duplicates | Allowed | Not allowed | Keys unique, values can duplicate |
| Order | Maintains insertion order | Depends on implementation | Depends on implementation |
| Access | By index | No index | By key |
| Common impl | `ArrayList`, `LinkedList` | `HashSet`, `LinkedHashSet` | `HashMap`, `LinkedHashMap` |
| Use case | Ordered collection, duplicates ok | Unique values, deduplication | Key-value lookup |

In this project:
- `List<Invoice>` — ordered list of invoices per customer
- `Set<Long>` — unique customer IDs with overdue invoices (deduplication automatic)
- `Map<InvoiceStatus, List<Invoice>>` — invoices grouped by status

---

**Q: What are Java Streams and when would you use them?**

Streams provide a functional, declarative way to process collections. They don't modify the original collection — they produce a new result.

Key operations:
- Intermediate (lazy, return a Stream): `filter`, `map`, `sorted`, `limit`, `distinct`
- Terminal (trigger execution): `collect`, `count`, `reduce`, `findFirst`, `anyMatch`, `allMatch`

```java
// Group invoices by status - groupingBy collector
Map<InvoiceStatus, List<Invoice>> grouped = invoices.stream()
    .collect(Collectors.groupingBy(Invoice::getStatus));

// Sum amounts for paid invoices
BigDecimal total = invoices.stream()
    .filter(i -> i.getStatus() == InvoiceStatus.PAID)
    .map(Invoice::getAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

// Top 3 invoices by amount
List<Invoice> top3 = invoices.stream()
    .sorted(Comparator.comparing(Invoice::getAmount).reversed())
    .limit(3)
    .toList();
```

Streams are lazy — intermediate operations don't execute until a terminal operation is called. `anyMatch` and `allMatch` short-circuit (stop early when result is known).

---

**Q: What is Optional and why use it?**

`Optional<T>` is a container that may or may not contain a value. It makes the possibility of absence explicit in the API, forcing the caller to handle the empty case rather than getting a `NullPointerException`.

```java
// Without Optional - NPE risk
Invoice invoice = repository.findById(id); // could return null
invoice.getAmount(); // NPE if null

// With Optional - explicit handling required
Optional<Invoice> invoice = repository.findById(id);
invoice.ifPresent(i -> log.info("Found: {}", i.getAmount()));

// orElseThrow - throw if empty
Invoice invoice = repository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));

// orElse - default value
BigDecimal amount = findHighestValueInvoice()
    .map(Invoice::getAmount)
    .orElse(BigDecimal.ZERO);
```

---

**Q: Why should you never use double or float for financial calculations?**

`double` and `float` use binary floating point which cannot represent all decimal fractions exactly. For example `0.1 + 0.2` in double arithmetic gives `0.30000000000000004`.

Always use `BigDecimal` for money:
```java
// Wrong - precision loss
double total = 0.1 + 0.2; // 0.30000000000000004

// Correct
BigDecimal total = new BigDecimal("0.1").add(new BigDecimal("0.2")); // 0.3

// Division requires explicit scale and rounding
BigDecimal average = total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
```

Always construct `BigDecimal` from a `String`, not a `double` — `new BigDecimal(0.1)` still has the floating point imprecision.

---

**Q: What is a Java record?**

A record is a concise way to declare an immutable data class. The compiler automatically generates the constructor, getters, `equals`, `hashCode`, and `toString`.

```java
// Record declaration
public record InvoiceSummary(
    long totalInvoices,
    long paidInvoices,
    BigDecimal totalValue
) {}

// Usage - access via component name (no "get" prefix)
InvoiceSummary summary = new InvoiceSummary(10, 7, new BigDecimal("5000.00"));
summary.totalInvoices(); // 10
summary.paidInvoices();  // 7
```

Records are immutable — no setters. Good for DTOs, value objects, and return types. Used in this project for `CustomerRequest`, `CustomerResponse`, and `InvoiceSummary`.

Introduced as a preview in Java 14, finalised in Java 16.

---

**Q: What is the difference between a lambda and a method reference?**

Both are ways to pass behaviour as a parameter. Method references are shorthand for lambdas that simply call an existing method.

```java
// Lambda
invoices.stream().map(invoice -> invoice.getAmount())

// Method reference - cleaner when lambda just calls one method
invoices.stream().map(Invoice::getAmount)

// Types of method references:
Invoice::getAmount          // instance method on parameter
BigDecimal::add             // instance method on first parameter (used in reduce)
UUID::randomUUID            // static method
new Customer()::getName     // instance method on specific object
```

Use method references when they make the code more readable. Use lambdas when you need logic beyond a single method call.
