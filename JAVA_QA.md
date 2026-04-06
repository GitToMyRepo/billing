# Core Java Q&A

Java-specific questions and answers for interview preparation.
For Spring Boot, CI/CD, and testing topics see [INTERVIEW_QA.md](INTERVIEW_QA.md).
For Spring & Spring Boot questions see [SPRING_QA.md](SPRING_QA.md).

> All concepts in this file have executable examples in
> `src/test/java/com/mywork/billing/examples/JavaConceptsExamplesTest.java`
> Run with: `./mvnw test -Dtest=JavaConceptsExamplesTest`
>
> For real billing domain examples using streams, collectors, Optional and records see:
> `src/main/java/com/mywork/billing/service/BillingReportService.java`
> and its tests in `src/test/java/com/mywork/billing/service/BillingReportServiceTest.java`

---

## Collections — List, Set, Map

**Q: Are List, Set and Map collections?**

Yes. They are all part of the Java Collections Framework:

```
java.util.Collection (interface)
    ├── List  — ordered, duplicates allowed  (ArrayList, LinkedList)
    ├── Set   — unordered, no duplicates     (HashSet, LinkedHashSet, TreeSet)
    └── Queue — FIFO ordering                (LinkedList, PriorityQueue)

java.util.Map (NOT a Collection but part of the framework)
    ├── HashMap        — unordered key-value pairs
    ├── LinkedHashMap  — insertion-ordered key-value pairs
    └── TreeMap        — sorted by key
```

`Map` is technically not a `Collection` because it stores key-value pairs rather than individual elements, but it is always grouped with collections in interviews.

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

## Streams, Lambdas & Method References

**Q: What are Java Streams and when would you use them?**

Streams provide a functional, declarative way to process collections. They don't modify the original collection — they always produce a new result.

Key operations:
- Intermediate (lazy, return a Stream): `filter`, `map`, `sorted`, `limit`, `distinct`
- Terminal (trigger execution): `collect`, `count`, `reduce`, `findFirst`, `anyMatch`, `allMatch`

**`filter`** — keeps elements that match a condition (same type in, same type out):
```java
// keep only PAID invoices
List<Invoice> paid = invoices.stream()
    .filter(i -> i.getStatus() == InvoiceStatus.PAID)
    .toList();
```

**`map`** — transforms each element into something else (one type in, different type out):
```java
// map Invoice to its amount (entity -> field)
List<BigDecimal> amounts = invoices.stream()
    .map(Invoice::getAmount)
    .toList();

// map entity to DTO (most common use in Spring Boot)
List<CustomerResponse> responses = customers.stream()
    .map(CustomerResponse::from)
    .toList();

// map String to uppercase
List<String> upper = names.stream()
    .map(String::toUpperCase)
    .toList();

// map String to its length (String -> Integer)
List<Integer> lengths = names.stream()
    .map(String::length)
    .toList();
```

**`sorted`** — sorts elements:
```java
// sort invoices by amount ascending
List<Invoice> sorted = invoices.stream()
    .sorted(Comparator.comparing(Invoice::getAmount))
    .toList();

// sort descending
List<Invoice> sortedDesc = invoices.stream()
    .sorted(Comparator.comparing(Invoice::getAmount).reversed())
    .toList();
```

**`collect`** — terminal operation, gathers results:
```java
// to List
List<Invoice> list = invoices.stream().filter(...).toList();

// to Map - group by status
Map<InvoiceStatus, List<Invoice>> byStatus = invoices.stream()
    .collect(Collectors.groupingBy(Invoice::getStatus));

// to Set - unique customer IDs
Set<Long> customerIds = invoices.stream()
    .map(i -> i.getCustomer().getId())
    .collect(Collectors.toSet());
```

**`reduce`** — combines all elements into one value:
```java
BigDecimal total = invoices.stream()
    .map(Invoice::getAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);
```

**`anyMatch` / `allMatch` / `noneMatch`** — short-circuit boolean checks:
```java
boolean hasOverdue = invoices.stream()
    .anyMatch(i -> i.getStatus() == InvoiceStatus.OVERDUE);

boolean allPaid = invoices.stream()
    .allMatch(i -> i.getStatus() == InvoiceStatus.PAID);
```

---

**Q: Is `collect` the terminal operation in a stream pipeline?**

Yes. `collect` is a terminal operation — it triggers execution of the entire pipeline. Everything before it is just building a description of what to do. Nothing actually runs until the terminal operation is called.

```java
invoices.stream()                    // creates a stream - nothing runs yet
    .filter(...)                     // describes a filter - nothing runs yet
    .map(...)                        // describes a mapping - nothing runs yet
    .collect(Collectors.toList())    // TERMINAL - now everything runs
```

Streams are lazy — intermediate operations don't execute until a terminal operation is called. `anyMatch` and `allMatch` short-circuit (stop early when result is known).

---

**Q: What does `reduce` do and how does it work?**

`reduce` combines all elements into a single value by repeatedly applying a function. It is equivalent to a for loop:

```java
// Stream reduce version
BigDecimal total = invoices.stream()
    .map(Invoice::getAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

// Equivalent for loop
BigDecimal total = BigDecimal.ZERO;
for (Invoice invoice : invoices) {
    total = total.add(invoice.getAmount());
}
```

Step by step for invoices of 100, 200, 300:
```
Start:    0.00
+ 100.00 = 100.00
+ 200.00 = 300.00
+ 300.00 = 600.00  ← final result
```

Another example — finding the maximum:
```java
BigDecimal max = invoices.stream()
    .map(Invoice::getAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::max);
```

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

---

## Optional

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

## BigDecimal & Financial Calculations

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

**Q: What does the `scale` parameter mean in `BigDecimal.divide`?**

Scale = number of decimal places in the result.

```java
total.divide(
    BigDecimal.valueOf(invoices.size()),
    2,                    // scale = 2 decimal places
    RoundingMode.HALF_UP
);
```

So `600.00 / 3 = 200.00` — result has exactly 2 decimal places.

---

**Q: What are the BigDecimal RoundingModes and when do you use them?**

`UP` and `DOWN` always round regardless of the fraction value:
- `UP` — always rounds away from zero: 2.341 → 2.35, 2.349 → 2.35
- `DOWN` — always truncates: 2.341 → 2.34, 2.349 → 2.34

`HALF_UP` and `HALF_DOWN` only differ at exactly 0.5:
- `HALF_UP` — rounds up if fraction ≥ 0.5 (standard maths): 2.345 → 2.35, 2.344 → 2.34
- `HALF_DOWN` — rounds up only if fraction > 0.5: 2.345 → 2.34, 2.346 → 2.35

`HALF_EVEN` (banker's rounding) — only behaves differently at exactly 0.5. Rounds to whichever side makes the last digit even:

| Value | Last digit | Even/Odd | HALF_EVEN | HALF_UP |
|-------|-----------|----------|-----------|---------|
| 2.25 | 2 | even | 2.2 (stay) | 2.3 |
| 2.35 | 3 | odd | 2.4 (round up) | 2.4 |
| 2.45 | 4 | even | 2.4 (stay) | 2.5 |
| 2.55 | 5 | odd | 2.6 (round up) | 2.6 |

HALF_EVEN alternates between rounding up and down at exactly 0.5, so rounding errors cancel out over many calculations rather than always accumulating in the same direction.

When to use:
- `HALF_UP` — most billing and everyday financial work, what people expect
- `HALF_EVEN` — banking and accounting systems summing millions of transactions
- `UP` — when you must never undercharge (e.g. tax calculations)
- `DOWN` — when you must never overcharge

---

## Records

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

## Generics

**Q: What are generics and why use them?**

Generics allow you to write type-safe code that works with different types without casting. The type is specified at compile time, catching errors early.

```java
// Without generics - requires casting, runtime ClassCastException risk
List list = new ArrayList();
list.add("hello");
String s = (String) list.get(0); // cast required

// With generics - type safe, no cast needed
// Diamond operator <> - compiler infers type from left side (List<String>)
// Equivalent to new ArrayList<String>() but cleaner - introduced in Java 7
List<String> list = new ArrayList<>();
list.add("hello");
String s = list.get(0); // no cast
```

---

**Q: What is the difference between `<? extends T>` and `<? super T>`?**

This is the PECS rule: **Producer Extends, Consumer Super**

`<? extends T>` — read only (producer):
- Accepts `T` or any subclass of `T`
- You can read from it but cannot add to it (except null)
- Reason: the compiler doesn't know the exact type. `List<? extends Number>` could be a `List<Integer>`, `List<Double>`, or `List<Float>`. Adding an `Integer` to what's actually a `List<Double>` would be wrong, so the compiler forbids all adds to be safe
- Use when you only need to get values out

```java
// extends - read only
List<? extends Number> numbers = new ArrayList<Integer>();
Number n = numbers.get(0); // OK - reading returns Number
numbers.add(1);            // COMPILE ERROR - can't add (could be List<Double>!)
```

`<? super T>` — write allowed (consumer):
- Accepts `T` or any superclass of `T`
- You can add `T` or subclasses of `T` to it
- Reading returns `Object` — because the list could be `List<Integer>`, `List<Number>`, or `List<Object>`, the compiler can only guarantee the element is at least an `Object`

```java
// super - can add
List<? super Integer> numbers = new ArrayList<Number>();
numbers.add(1);            // OK - can add Integer or subclass
Object o = numbers.get(0); // reading returns Object only - could be Number or Object
```

Simple rule:
- Need to **read** from collection → use `? extends`
- Need to **write** to collection → use `? super`
- Need to **both read and write** → use no wildcard

Billing example:
```java
// Read invoice amounts - extends (producer)
BigDecimal sum(List<? extends Invoice> invoices) {
    return invoices.stream().map(Invoice::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}

// Add invoices to a list - super (consumer)
void addInvoices(List<? super Invoice> target, List<Invoice> source) {
    target.addAll(source);
}
```

---

## Exception Handling

**Q: What is the difference between checked and unchecked exceptions?**

| | Checked | Unchecked |
|---|---|---|
| Extends | `Exception` | `RuntimeException` |
| Must handle? | Yes — compile error if not caught or declared | No — optional |
| Examples | `IOException`, `SQLException` | `NullPointerException`, `IllegalArgumentException` |
| Use case | Recoverable conditions (file not found, network error) | Programming errors (null access, invalid args) |

```java
// Checked - must catch or declare throws
public void readFile(String path) throws IOException {
    Files.readAllBytes(Path.of(path));
}

// Unchecked - no obligation to catch
public void process(String input) {
    if (input == null) throw new IllegalArgumentException("Input cannot be null");
}
```

In this project, `ResourceNotFoundException` and `DuplicateResourceException` extend `RuntimeException` (unchecked) — Spring's `@ControllerAdvice` catches them and maps to HTTP responses.

---

**Q: What is try-with-resources and why use it?**

Try-with-resources automatically closes resources that implement `AutoCloseable` when the try block exits — even if an exception is thrown. Introduced in Java 7.

```java
// Pre Java 7 - manual close in finally, verbose and error-prone
BufferedReader br = null;
try {
    br = new BufferedReader(new FileReader("file.txt"));
    // use br
} finally {
    if (br != null) br.close(); // easy to forget
}

// Java 7+ - automatic close, cleaner
try (BufferedReader br = new BufferedReader(new FileReader("file.txt"))) {
    // use br
} // br.close() called automatically
```

---

**Q: What is the difference between `final`, `finally`, and `finalize`?**

- `final` — makes a variable reference unchangeable, a method non-overridable, or a class non-inheritable
- `finally` — block that always executes after try/catch, used to release resources
- `finalize` — called by GC before object is collected. Deprecated since Java 9, should never be used

```java
final int MAX = 100;           // constant
final class ImmutableClass {}  // can't extend

try {
    // risky code
} finally {
    // always runs, even if exception thrown
}
```

---

## Garbage Collection & Memory

**Q: What are the Java memory areas (Stack vs Heap)?**

| | Stack | Heap |
|---|---|---|
| Stores | Local variables, method parameters, object references | Objects, instance variables |
| Scope | Per thread, per method call | Shared across all threads |
| Size | Small, fixed | Large, dynamic |
| Lifetime | Until method returns | Until GC collects |
| Speed | Fast | Slower |

```java
public void process() {
    int count = 5;               // primitive int - stored directly in stack
    String name = "billing";     // reference in stack, String object in heap (String pool)
    Invoice inv = new Invoice(); // reference in stack, Invoice object in heap
}
// when method returns, stack frame is popped:
// - count, name, inv references are gone from stack
// - Invoice object in heap is now eligible for GC (no more references)
// - "billing" stays in String pool until JVM decides to collect it
```

Visual:
```
Stack (per thread)          Heap (shared)
┌─────────────────┐        ┌──────────────────────────────┐
│ process() frame │        │  Invoice object               │
│  count = 5      │        │    id = null                  │
│  name ──────────┼───────►│  String pool: "billing"       │
│  inv ───────────┼───────►│  Invoice object               │
└─────────────────┘        └──────────────────────────────┘
```

Key interview points:
- Primitives (`int`, `boolean`, `double`) are stored directly in the stack
- Objects are always in the heap — the stack only holds a reference (pointer) to them
- Each thread has its own stack — thread safe for local variables
- The heap is shared — concurrent access to objects needs synchronisation

---

**Q: How does Garbage Collection work in Java?**

The JVM automatically reclaims memory from objects that are no longer reachable (no references pointing to them). You don't need to manually free memory like in C/C++.

Key concepts:
- **GC Roots** — starting points for reachability: local variables, static fields, active threads
- **Mark and Sweep** — GC marks all reachable objects, then sweeps (collects) unreachable ones
- **Generational GC** — heap is divided into Young Generation (new objects) and Old Generation (long-lived objects). Most objects die young — collecting Young Gen is fast

Heap generations:
```
Young Generation          Old Generation
┌──────────────────┐     ┌──────────────────┐
│ Eden  │ S0 │ S1  │ →   │   Tenured/Old    │
└──────────────────┘     └──────────────────┘
  New objects live here    Long-lived objects
  Minor GC - fast          Major GC - slow
```

- New objects created in Eden
- Surviving Minor GC moved to Survivor spaces (S0/S1)
- After several GC cycles, promoted to Old Generation
- Major GC (Full GC) collects Old Generation — causes "stop the world" pause

---

**Q: What causes memory leaks in Java and how do you prevent them?**

Java has GC but memory leaks still happen when objects are referenced but never used again.

Common causes:
- Static collections that grow indefinitely
- Listeners/callbacks not removed
- Caches without eviction policy
- MDC not cleaned up in thread pools (relevant to our `CorrelationIdFilter`)

```java
// Memory leak - static map grows forever
static Map<String, Object> cache = new HashMap<>();
cache.put(key, value); // never removed

// Fix - use WeakHashMap or add eviction
Map<String, Object> cache = Collections.synchronizedMap(
    new LinkedHashMap<>(100, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 100; // evict when over 100 entries
        }
    }
);
```

MDC leak example (from our project):
```java
MDC.put("correlationId", id);
try {
    filterChain.doFilter(request, response);
} finally {
    MDC.remove("correlationId"); // MUST clean up - thread pool reuses threads
}
```

---

## Java 21 Features

**Q: What are the key new features in Java 21?**

Java 21 is an LTS release. Key features:

**Virtual Threads (Project Loom)**
Lightweight threads managed by the JVM, not the OS. Allows millions of concurrent threads without the overhead of platform threads. Ideal for I/O-heavy applications like billing services making DB calls.

```java
// Platform thread - expensive, limited by OS
Thread.ofPlatform().start(() -> handleRequest());

// Virtual thread - cheap, JVM managed
Thread.ofVirtual().start(() -> handleRequest());

// Spring Boot 4 - enable virtual threads for all requests
spring.threads.virtual.enabled=true
```

**Sealed Classes**
Restrict which classes can extend or implement a type. Makes the type hierarchy explicit and exhaustive.

```java
// Only these three classes can implement InvoiceEvent
public sealed interface InvoiceEvent
    permits InvoiceCreated, InvoicePaid, InvoiceOverdue {}

public record InvoiceCreated(Long invoiceId, BigDecimal amount) implements InvoiceEvent {}
public record InvoicePaid(Long invoiceId, LocalDateTime paidAt) implements InvoiceEvent {}
public record InvoiceOverdue(Long invoiceId, LocalDate dueDate) implements InvoiceEvent {}

// Pattern matching switch - compiler knows all cases
String describe(InvoiceEvent event) {
    return switch (event) {
        case InvoiceCreated c -> "Created: " + c.invoiceId();
        case InvoicePaid p   -> "Paid: " + p.invoiceId();
        case InvoiceOverdue o -> "Overdue: " + o.invoiceId();
        // no default needed - compiler knows all cases are covered
    };
}
```

**Record Patterns (Java 21)**
Deconstruct records in pattern matching:

```java
// Pattern matching with records
if (event instanceof InvoiceCreated(Long id, BigDecimal amount)) {
    log.info("Invoice {} created for {}", id, amount);
}
```

**Sequenced Collections**
New interfaces `SequencedCollection`, `SequencedSet`, `SequencedMap` with methods like `getFirst()`, `getLast()`, `reversed()`.

```java
List<Invoice> invoices = new ArrayList<>();
Invoice first = invoices.getFirst(); // Java 21 - no more get(0)
Invoice last = invoices.getLast();   // Java 21 - no more get(size-1)
```

---

## String

**Q: What is the difference between `==` and `equals()` for Strings?**

`==` compares object references (are they the same object in memory?).
`equals()` compares the actual string content.

```java
String s1 = "Hello";              // creates "Hello" in String pool, s1 points to it
String s2 = new String("Hello");  // creates new object on heap, NOT in pool
String s3 = "Hello";              // "Hello" already in pool, s3 points to same object as s1

s1 == s2      // false - s1 points to pool, s2 points to heap object
s1.equals(s2) // true  - same content "Hello"
s1 == s3      // true  - both point to same pool object
```

String literals are interned — JVM keeps one copy in the String pool and reuses it. `new String("Hello")` always creates a new object bypassing the pool.

Always use `equals()` to compare String values, never `==`.

---

**Q: Why is String immutable in Java?**

Two reasons:

1. **Thread safety** — immutable objects can be safely shared across threads without synchronisation
2. **String pool** — immutability makes the String pool possible. If strings were mutable, changing one reference would corrupt all other references pointing to the same pooled string

Also enables safe use as HashMap keys — the hashcode never changes.

---

**Q: What is the difference between overloading and overriding?**

| | Overloading | Overriding |
|---|---|---|
| Where | Same class, different signatures | Parent and child class, same signature |
| Resolved at | Compile time | Runtime |
| Also called | Compile-time polymorphism | Runtime polymorphism |

```java
// Overloading - same name, different params
void process(String input) {}
void process(int input) {}

// Overriding - child replaces parent behaviour
class Animal { void speak() { System.out.println("..."); } }
class Dog extends Animal {
    @Override void speak() { System.out.println("Woof"); }
}
```

Overriding is determined by the **stored** object type, not the reference type:
```java
Animal a = new Dog();
a.speak(); // prints "Woof" - Dog's method called at runtime
```

---

## Static Variables, Methods & Memory

**Q: How do static variables and methods affect memory and garbage collection?**

Static variables belong to the class, not instances. They live in the heap (Metaspace area) for the **entire lifetime of the application**. The JVM treats them as GC roots — GC will never collect them or anything they reference.

```java
// Static variable - lives forever, never GC'd
public class InvoiceCache {
    private static final Map<Long, Invoice> cache = new HashMap<>(); // lives forever

    public static void add(Long id, Invoice invoice) {
        cache.put(id, invoice); // anything added here stays in memory forever
    }
}
```

Static methods don't affect memory directly — they have no instance state and execute on the stack like any other method.

**Risk:** Static collections that grow without bounds are the most common memory leak in Java.

---

**Q: GC is controlled by the JVM — what should developers watch out for?**

You're right — `System.gc()` is just a hint, the JVM can ignore it. You cannot force GC. What you should watch out for as a developer:

| Cause | Example | Fix |
|-------|---------|-----|
| Static collections growing unbounded | `static Map<Long, Invoice> cache` never cleared | Use bounded cache with eviction |
| Listeners never removed | Event listener registered but never deregistered | Remove listener when done |
| ThreadLocal not cleaned up | MDC not cleared in thread pool | Always clean up in `finally` |
| Unbounded cache | `HashMap` used as cache, never evicted | Use `WeakHashMap` or Caffeine/Guava cache |
| Inner class holding outer reference | Anonymous inner class captures outer `this` | Use static inner class or lambda |

The `CorrelationIdFilter` in this project is a good example of correct ThreadLocal cleanup:
```java
MDC.put("correlationId", id);
try {
    filterChain.doFilter(request, response);
} finally {
    MDC.remove("correlationId"); // MUST clean up - thread pool reuses threads
}
```

---

**Q: Are Map keys mutable? Should they be?**

Technically you can use a mutable object as a key, but you should **never** do it.

`HashMap` uses the key's `hashCode()` to find the correct bucket. If you mutate the key after inserting it, its `hashCode()` changes — the map can no longer find the entry. It's effectively lost in the map, causing a memory leak and incorrect behaviour.

```java
// WRONG - mutable key
List<String> key = new ArrayList<>();
key.add("INV-001");
Map<List<String>, BigDecimal> map = new HashMap<>();
map.put(key, new BigDecimal("100.00"));

key.add("INV-002"); // mutate the key - hashCode changes!
map.get(key);       // returns null - can't find it anymore!

// CORRECT - immutable keys
Map<String, BigDecimal> map = new HashMap<>();
map.put("INV-001", new BigDecimal("100.00")); // String is immutable - safe
map.put(1L, invoice);                          // Long is immutable - safe
```

Always use immutable objects as Map keys: `String`, `Integer`, `Long`, enums, records.

---

## Collectors

**Q: What is a Collector and how is it used with streams?**

`Collector` is an interface that defines how to accumulate stream elements into a result container. `Collectors` (plural, with an 's') is a utility class with factory methods for the most common collectors. You pass a `Collector` to `collect()` as the terminal operation.

```java
// toList - accumulate into a List
List<Invoice> pending = invoices.stream()
    .filter(i -> i.getStatus() == InvoiceStatus.PENDING)
    .collect(Collectors.toList());
// or Java 16+ shorthand
List<Invoice> pending = invoices.stream()
    .filter(i -> i.getStatus() == InvoiceStatus.PENDING)
    .toList();

// toSet - accumulate into a Set (deduplicates)
Set<Long> customerIds = invoices.stream()
    .map(i -> i.getCustomer().getId())
    .collect(Collectors.toSet());

// groupingBy - group into Map<Key, List<Value>>
Map<InvoiceStatus, List<Invoice>> byStatus = invoices.stream()
    .collect(Collectors.groupingBy(Invoice::getStatus));

// groupingBy with downstream collector - count per status
Map<InvoiceStatus, Long> countByStatus = invoices.stream()
    .collect(Collectors.groupingBy(Invoice::getStatus, Collectors.counting()));

// groupingBy with mapping - get invoice numbers per customer
Map<Long, List<String>> numbersByCustomer = invoices.stream()
    .collect(Collectors.groupingBy(
        i -> i.getCustomer().getId(),
        Collectors.mapping(Invoice::getInvoiceNumber, Collectors.toList())
    ));

// toMap - custom key-value pairs
Map<Long, BigDecimal> amountById = invoices.stream()
    .collect(Collectors.toMap(Invoice::getId, Invoice::getAmount));

// joining - concatenate strings
String invoiceNumbers = invoices.stream()
    .map(Invoice::getInvoiceNumber)
    .collect(Collectors.joining(", ")); // "INV-001, INV-002, INV-003"

// summarizingDouble - statistics in one pass
DoubleSummaryStatistics stats = invoices.stream()
    .collect(Collectors.summarizingDouble(i -> i.getAmount().doubleValue()));
// stats.getAverage(), stats.getMax(), stats.getMin(), stats.getSum()
```
