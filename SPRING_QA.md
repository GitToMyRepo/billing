# Spring & Spring Boot Q&A

Spring-specific questions and answers for interview preparation.
For Core Java questions see [JAVA_QA.md](JAVA_QA.md).
For CI/CD, testing and tooling see [INTERVIEW_QA.md](INTERVIEW_QA.md).

---

## Spring MVC Annotations

**Q: What is the difference between `@Controller` and `@RestController`?**

`@Controller` is the base annotation for Spring MVC controllers. By default it expects the method to return a view name (e.g. a Thymeleaf template). To return data directly in the response body you must add `@ResponseBody` to each method.

`@RestController` is a convenience annotation that combines `@Controller` + `@ResponseBody`. Every method automatically serialises the return value to JSON (or XML) and writes it directly to the HTTP response body. No view resolution happens.

```java
// @Controller - returns a view name by default
@Controller
public class PageController {
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        return "dashboard"; // resolves to dashboard.html template
    }

    // Need @ResponseBody to return data instead of a view
    @GetMapping("/api/data")
    @ResponseBody
    public CustomerResponse getData() {
        return new CustomerResponse(...);
    }
}

// @RestController - always returns data, never a view
@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    @GetMapping("/{id}")
    public CustomerResponse getCustomer(@PathVariable Long id) {
        return customerService.getCustomerById(id); // serialised to JSON automatically
    }
}
```

In modern Spring Boot REST APIs you always use `@RestController`. `@Controller` is used when building server-side rendered web applications with templates (Thymeleaf, JSP).

---

**Q: What does `@RequestMapping` do and what are the shortcut annotations?**

`@RequestMapping` maps HTTP requests to handler methods. It can be placed on the class (base path) and on methods (specific path + HTTP method).

Shortcut annotations for specific HTTP methods:

| Annotation | Equivalent |
|-----------|-----------|
| `@GetMapping` | `@RequestMapping(method = GET)` |
| `@PostMapping` | `@RequestMapping(method = POST)` |
| `@PutMapping` | `@RequestMapping(method = PUT)` |
| `@DeleteMapping` | `@RequestMapping(method = DELETE)` |
| `@PatchMapping` | `@RequestMapping(method = PATCH)` |

```java
@RestController
@RequestMapping("/api/customers")  // base path for all methods
public class CustomerController {

    @GetMapping("/{id}")            // GET /api/customers/{id}
    public CustomerResponse get(@PathVariable Long id) { ... }

    @PostMapping                    // POST /api/customers
    public ResponseEntity<CustomerResponse> create(@RequestBody CustomerRequest req) { ... }

    @PutMapping("/{id}")            // PUT /api/customers/{id}
    public CustomerResponse update(@PathVariable Long id, @RequestBody CustomerRequest req) { ... }

    @DeleteMapping("/{id}")         // DELETE /api/customers/{id}
    public ResponseEntity<Void> delete(@PathVariable Long id) { ... }
}
```

---

**Q: What is the difference between `@PathVariable`, `@RequestParam`, and `@RequestBody`?**

| Annotation | Where data comes from | Example URL |
|-----------|----------------------|-------------|
| `@PathVariable` | URL path segment | `/api/customers/123` |
| `@RequestParam` | URL query string | `/api/customers?status=PAID` |
| `@RequestBody` | HTTP request body (JSON) | POST body |

```java
// @PathVariable - /api/customers/123
@GetMapping("/{id}")
public CustomerResponse get(@PathVariable Long id) { ... }

// @RequestParam - /api/invoices?status=PAID&page=0
@GetMapping
public List<Invoice> list(
    @RequestParam InvoiceStatus status,
    @RequestParam(defaultValue = "0") int page) { ... }

// @RequestBody - POST with JSON body
@PostMapping
public CustomerResponse create(@Valid @RequestBody CustomerRequest request) { ... }
```

---

## Spring Beans & Dependency Injection

**Q: What is a Spring Bean?**

A Spring Bean is an object managed by the Spring IoC (Inversion of Control) container. Spring creates, configures, and manages the lifecycle of beans. You declare beans using annotations:

- `@Component` — generic bean
- `@Service` — service layer bean (same as @Component, semantic meaning)
- `@Repository` — data access layer bean (adds exception translation)
- `@Controller` / `@RestController` — web layer bean
- `@Configuration` + `@Bean` — explicit bean definition

---

**Q: What is the difference between `@Component`, `@Service`, and `@Repository`?**

Functionally they are all the same — all register a bean in the Spring context. The difference is semantic and tooling:

- `@Component` — generic, use when none of the others fit
- `@Service` — marks business logic layer, makes intent clear
- `@Repository` — marks data access layer, additionally enables Spring's persistence exception translation (converts database exceptions to Spring's `DataAccessException` hierarchy)

---

**Q: What are the types of dependency injection and which is preferred?**

Three types:

```java
// 1. Constructor injection - PREFERRED
@Service
public class CustomerService {
    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) { // Spring injects via constructor
        this.repository = repository;
    }
}

// 2. Field injection - NOT recommended
@Service
public class CustomerService {
    @Autowired
    private CustomerRepository repository; // Spring injects directly into field
}

// 3. Setter injection - rarely used
@Service
public class CustomerService {
    private CustomerRepository repository;

    @Autowired
    public void setRepository(CustomerRepository repository) {
        this.repository = repository;
    }
}
```

Constructor injection is preferred because:
- Dependencies are explicit and required — can't create the object without them
- Works with `final` fields — makes the class immutable
- Easier to test — just pass mocks in the constructor
- No need for `@Autowired` in Spring 4.3+ if there's only one constructor

---

**Q: What is `@Transactional` and when should you use it?**

`@Transactional` wraps a method in a database transaction. If the method completes successfully, the transaction commits. If a `RuntimeException` is thrown, it rolls back.

```java
@Service
@Transactional(readOnly = true) // default all methods to read-only
public class CustomerService {

    @Transactional // override for write operations
    public CustomerResponse createCustomer(CustomerRequest request) {
        // if exception thrown here, transaction rolls back
        Customer saved = customerRepository.save(customer);
        auditRepository.save(new AuditLog("CREATED", saved.getId())); // same transaction
        return CustomerResponse.from(saved);
    }
}
```

`readOnly = true` is a performance hint — no dirty checking, no flush. Use it on all read methods.

Without `@Transactional` on a service method that does multiple DB operations, each operation runs in its own transaction — a failure halfway through leaves data in an inconsistent state.

---

**Q: What is `@ControllerAdvice` and `@ExceptionHandler`?**

`@ControllerAdvice` (or `@RestControllerAdvice`) is a global exception handler that applies across all controllers. `@ExceptionHandler` maps a specific exception type to a handler method.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // collect field errors from @Valid validation
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Validation failed", errors));
    }
}
```

Without `@ControllerAdvice`, you'd need try/catch in every controller method. With it, exception handling is centralised in one place.

---

## MDC & ThreadLocal

**Q: Is MDC backed by ThreadLocal?**

Yes. SLF4J's MDC (Mapped Diagnostic Context) uses a `ThreadLocal<Map<String, String>>` internally — each thread gets its own independent copy of the map. This is why correlation IDs from different requests don't interfere with each other.

This is also why you **must** clean up MDC in a `finally` block. In a thread pool (like Tomcat's), threads are reused across requests. If you don't remove the value, the next request on that thread inherits the previous request's correlation ID — a subtle but serious bug.

```java
MDC.put("correlationId", id);
try {
    filterChain.doFilter(request, response);
} finally {
    MDC.remove("correlationId"); // MUST clean up - thread pool reuses threads
}
```

**Q: What is ThreadLocal?**

`ThreadLocal<T>` provides a variable where each thread has its own independent copy. Changes in one thread are invisible to other threads.

```java
ThreadLocal<String> requestId = new ThreadLocal<>();

// Thread 1
requestId.set("req-001");
requestId.get(); // "req-001"

// Thread 2 (simultaneously)
requestId.set("req-002");
requestId.get(); // "req-002" - completely independent

// Always clean up in finally to prevent memory leaks in thread pools
requestId.remove();
```

Common uses: request context, user session, correlation IDs (MDC), database connections per thread.

---

## Configuration

**Q: When do you need a `@Configuration` class in Spring Boot?**

Spring Boot auto-configures most things from `application.properties`. You add a `@Configuration` class when you need to customise beyond what properties support or define beans explicitly.

Common use cases:

| Use case | Example |
|----------|---------|
| Custom Jackson serialisation | Date format, null handling |
| Multiple datasources | Read/write splitting |
| Security configuration | Spring Security filter chain |
| Custom bean definitions | Third-party library integration |
| Feature flags | Conditional beans |

In this project, `JacksonConfig` configures date serialisation:

```java
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
```

Without this, `LocalDateTime` serialises as an array `[2026, 4, 5, 21, 54, 51]`.
With this, it serialises as an ISO string `"2026-04-05T21:54:51"`.

---

**Q: Why is `@Autowired` not used in the service and controller classes?**

In Spring 4.3+, if a class has exactly one constructor, Spring automatically injects dependencies through it — no `@Autowired` annotation needed. Constructor injection is preferred:

```java
// No @Autowired needed - Spring injects automatically via the single constructor
@Service
public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
}
```

`@Autowired` on fields is still used in test classes because JUnit creates the test instance and Spring injects into it after creation:

```java
@SpringBootTest
class CustomerIntegrationTest {
    @Autowired
    private MockMvc mockMvc; // field injection needed in tests
}
```

---

**Q: What is read/write datasource splitting and why is it used?**

Read/write splitting routes database connections based on the transaction type:
- `@Transactional(readOnly = true)` → read replica (SELECT only)
- `@Transactional` → primary database (INSERT, UPDATE, DELETE)

Benefits:
- Reduces load on the primary database
- Read replicas can scale horizontally
- Better performance for read-heavy systems like billing (lots of invoice queries)

In this project, `DatabaseConfig` implements this using Spring's `AbstractRoutingDataSource`. It's disabled by default (`@ConditionalOnProperty`) to avoid conflicting with Spring Boot's auto-configuration, but the pattern is there as a reference.

```java
@Configuration
@ConditionalOnProperty(name = "billing.datasource.routing.enabled", havingValue = "true")
public class DatabaseConfig {

    // Routing logic - checks current transaction's readOnly flag
    static class RoutingDataSource extends AbstractRoutingDataSource {
        @Override
        protected Object determineCurrentLookupKey() {
            boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            return isReadOnly ? DataSourceType.READ_ONLY : DataSourceType.PRIMARY;
        }
    }
}
```

`CustomerService` uses `@Transactional(readOnly = true)` as the class default, so all GET operations automatically route to the read replica when this config is enabled.

---

**Q: What is the difference between `application.properties` and a Kubernetes deployment YAML?**

These are two completely different things that are often confused:

`application.properties` (or `application.yml`) — Spring Boot application configuration:
- Log levels, Hibernate SQL logging, server port
- Feature flags, cache settings, pagination defaults
- Lives in `src/main/resources`, versioned with the code

Kubernetes deployment YAML — infrastructure deployment manifest:
- Tells Kubernetes how to run the container (image, replicas, resources)
- Injects environment variables into the container (database URLs, SNS/SQS endpoints, service URLs, secrets)
- The Spring app reads these injected env vars at startup via `${ENV_VAR_NAME}` in properties

```yaml
# Kubernetes deployment.yml - infrastructure config
env:
  - name: SPRING_DATASOURCE_URL
    value: jdbc:postgresql://prod-db:5432/billingdb
  - name: SPRING_DATASOURCE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: db-secret
        key: password
```

```properties
# application.properties - app config
spring.jpa.show-sql=false
logging.level.com.mywork=INFO
spring.jpa.properties.hibernate.format_sql=true
```

The Kubernetes YAML overrides Spring properties via environment variables — `SPRING_DATASOURCE_URL` maps to `spring.datasource.url` automatically (Spring Boot converts env var naming convention).

---

**Q: What is `@ConfigurationProperties` and how does it compare to `@Value`?**

Both bind configuration values from `application.properties` or `application.yml` to Java fields. `@ConfigurationProperties` is preferred for groups of related properties.

```java
// @Value - one property at a time, verbose for many properties
@Value("${spring.datasource.url}")
private String dbUrl;

// @ConfigurationProperties - binds a whole group at once
@Component
@ConfigurationProperties(prefix = "billing.datasource")
public class DataSourceConfig {
    private String url;
    private String username;
    private String password;
    // getters and setters
}
```

Equivalent `application.yml`:
```yaml
billing:
  datasource:
    url: jdbc:postgresql://localhost:5432/billingdb
    username: billing
    password: billing123
```

In production, sensitive values like passwords would be injected as environment variables rather than hardcoded in yml files.

---

## Kubernetes Deployment

**Q: How does a Spring Boot app get its configuration in Kubernetes?**

In Kubernetes, configuration is injected into the container as environment variables. Spring Boot automatically maps environment variables to properties using a naming convention — `SPRING_DATASOURCE_URL` maps to `spring.datasource.url`.

```yaml
# k8s/billing-api-deployment.yml
env:
  - name: SPRING_DATASOURCE_URL
    value: jdbc:postgresql://billing-postgres:5432/billingdb
  - name: SPRING_DATASOURCE_PASSWORD
    valueFrom:
      secretKeyRef:          # read from a Kubernetes Secret
        name: billing-db-secret
        key: password
```

Sensitive values (passwords, API keys) are stored in Kubernetes Secrets, not hardcoded in the deployment YAML. The Secret values are base64 encoded.

**Q: What are liveness and readiness probes?**

Kubernetes uses these to manage pod health:

- `livenessProbe` — is the app alive? If it fails, Kubernetes restarts the pod
- `readinessProbe` — is the app ready to receive traffic? If it fails, Kubernetes removes the pod from the load balancer but doesn't restart it

Spring Boot Actuator exposes these automatically:
```
GET /actuator/health/liveness   → livenessState
GET /actuator/health/readiness  → readinessState
```

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30   # wait 30s before first check (app startup time)
  periodSeconds: 10          # check every 10s

livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 30
```

---

**Q: How do you deploy a Spring Boot app to Kubernetes (Docker Desktop)?**

Prerequisites:
- Docker Desktop with Kubernetes enabled (Settings → Kubernetes → Enable Kubernetes)
- Docker image built locally

Steps:
```bash
# 1. Build the JAR
./mvnw package -DskipTests

# 2. Build the Docker image (imagePullPolicy: Never uses local image)
docker build -t billing-api:latest .

# 3. Apply manifests in order - secret first, then dependencies, then app
kubectl apply -f k8s/billing-secret.yml
kubectl apply -f k8s/postgres-deployment.yml
kubectl apply -f k8s/billing-api-deployment.yml

# 4. Check pods are running
kubectl get pods

# 5. Check logs if pod is not starting
kubectl logs -f deployment/billing-api
```

Expected output when healthy:
```
NAME                                READY   STATUS    RESTARTS   AGE
billing-api-865484f786-8tttd        1/1     Running   1          107s
billing-postgres-5c4475bbfc-xpz2q   1/1     Running   0          107s
```

Access the app at `http://localhost:30080` (NodePort 30080 defined in the Service manifest).

The `billing-api` pod may restart once on first start — this is normal if it starts before Postgres is fully ready. Kubernetes automatically restarts it and it comes up healthy on the second attempt. In production you'd use an init container or retry logic to handle this more gracefully.

**Q: What is `imagePullPolicy: Never` in the Kubernetes deployment?**

By default Kubernetes tries to pull images from a registry (Docker Hub, ECR etc.). `imagePullPolicy: Never` tells Kubernetes to only use locally available images — required when using images built on your local machine with Docker Desktop.

In production this would be `imagePullPolicy: Always` or `IfNotPresent`, pulling from a real registry like ECR or Docker Hub.

**Q: What is a NodePort service?**

A `NodePort` service exposes the application on a static port on every node in the cluster. With Docker Desktop's single-node cluster, this means the app is accessible on `localhost:<nodePort>`.

```yaml
spec:
  type: NodePort
  ports:
    - port: 8080        # internal cluster port
      targetPort: 8080  # container port
      nodePort: 30080   # external port on the node (localhost:30080)
```

In production you'd use a `LoadBalancer` service (cloud provider creates a load balancer) or an `Ingress` controller for HTTP routing.

---

**Q: Does Spring automatically generate HTTP response codes, or do you need to control them yourself?**

Both — it depends on the type of error:

**Spring generates automatically:**
- `404` — when no handler mapping matches the URL (e.g. `/api/nonexistent`)
- `405` — when the HTTP method isn't supported on an endpoint
- `400` — for malformed JSON, but with Spring's default generic error body

**Controlled by `@ControllerAdvice` / `GlobalExceptionHandler`:**
- `404` when a customer isn't found — Spring doesn't know about your business logic. Without the handler, `ResourceNotFoundException` would cause a `500`
- `409` for duplicate email — Spring has no idea this should be a conflict
- `400` for validation failures — Spring generates the 400 but the response body would be Spring's default format, not your custom `ErrorResponse`
- `400` for malformed JSON — override Spring's default to return your consistent error format

The `GlobalExceptionHandler` does two things:
1. Maps business exceptions to the correct HTTP status codes
2. Controls the response body format so all errors look consistent

Without it, your API would return `500` for most business errors and the error format would be Spring's default which is harder for API consumers to work with.

> "Spring handles some HTTP errors automatically like 404 for unknown routes and 405 for wrong methods. But for business logic errors like resource not found or duplicate data, we use `@ControllerAdvice` to map our custom exceptions to the appropriate HTTP status codes and return a consistent error response format."
