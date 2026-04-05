# Interview Q&A

A running log of technical questions and answers covered during interview preparation.
For skills and experience talking points, see [SKILLS_AND_EXPERIENCE.md](SKILLS_AND_EXPERIENCE.md).

---

## Spring Boot

**Q: Do you have to wrap the return object in `ResponseEntity` in a Spring Boot controller?**

No. You can return the object directly and Spring will serialize it to JSON with `200 OK` automatically:

```java
// Without ResponseEntity - Spring defaults to 200 OK
@GetMapping("/{id}")
public CustomerResponse getCustomerById(@PathVariable Long id) {
    return customerService.getCustomerById(id);
}

// With ResponseEntity - explicit control over status code and headers
@GetMapping("/{id}")
public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
    return ResponseEntity.ok(customerService.getCustomerById(id));
}
```

`ResponseEntity` gives you explicit control over the HTTP status code and headers. Without it you cannot return `201 Created` — Spring always defaults to `200`. You can work around that with `@ResponseStatus(HttpStatus.CREATED)` on the method, but `ResponseEntity` is more flexible.

Rule of thumb:
- GET — returning the object directly is fine, always `200`
- POST — use `ResponseEntity` or `@ResponseStatus` to return `201 Created`
- DELETE — use `ResponseEntity<Void>` or `@ResponseStatus(HttpStatus.NO_CONTENT)` since there is no body

---

**Q: Does a POST endpoint have to return something?**

No. You can return `void` with `@ResponseStatus(HttpStatus.CREATED)`. However, returning the created resource in the response body is REST best practice — the caller immediately gets the `id` and `createdAt` without needing a second GET request. Most real-world APIs follow this pattern.

---

**Q: How does Spring Boot know to return JSON from a controller?**

Spring Boot auto-configures `Jackson` (via `spring-boot-starter-web`) as the default message converter. When a controller method returns an object and the request has `Accept: application/json` (or no Accept header), Spring automatically serializes the response to JSON. You can also annotate the class or method with `@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)` to be explicit, but it is not required.

---

**Q: Why use `@Transactional` on some service methods?**

The class is annotated `@Transactional(readOnly = true)` as the default — this opens a read-only transaction for every method, which is a performance hint to the database (no dirty checking, no flush). Write methods (`createCustomer`, `updateCustomer`, `deleteCustomer`) override this with `@Transactional` (without `readOnly`) because they need a full read-write transaction to commit changes.

Without `@Transactional` on write operations, JPA may still work in simple cases because Spring Data's `save()` method has its own `@Transactional`. However, if your service method does multiple operations (e.g. save + update another entity), they won't be wrapped in a single transaction — meaning a failure halfway through could leave data in an inconsistent state. Explicit `@Transactional` on the service method ensures atomicity.

---

**Q: What are the HTTP methods and when do you use them?**

| Method | Description | Idempotent | Request Body |
|--------|-------------|------------|--------------|
| GET | Retrieve a resource or list of resources | Yes | No |
| POST | Create a new resource | No | Yes |
| PUT | Replace a resource entirely | Yes | Yes |
| PATCH | Partially update a resource | No | Yes |
| DELETE | Remove a resource | Yes | No |

Idempotent means calling it multiple times produces the same result — e.g. deleting the same resource twice still results in it being deleted.

---

**Q: What are the most common HTTP response codes?**

| Code | Name | Meaning |
|------|------|---------|
| 200 | OK | Request succeeded, response body contains the result |
| 201 | Created | Resource was successfully created (used with POST) |
| 204 | No Content | Request succeeded but no body to return (used with DELETE) |
| 400 | Bad Request | Invalid request — e.g. failed validation, malformed JSON |
| 401 | Unauthorized | Not authenticated — no or invalid credentials provided |
| 403 | Forbidden | Authenticated but not authorised to access the resource |
| 404 | Not Found | Resource does not exist |
| 405 | Method Not Allowed | HTTP method not supported on this endpoint |
| 409 | Conflict | Request conflicts with current state — e.g. duplicate email |
| 422 | Unprocessable Entity | Semantically invalid request — similar to 400 but more specific |
| 500 | Internal Server Error | Unexpected server-side error |
| 502 | Bad Gateway | Upstream service returned an invalid response |
| 503 | Service Unavailable | Server is temporarily unavailable — e.g. overloaded or down |
| 504 | Gateway Timeout | Upstream service did not respond in time |

---

## Testing

**Q: Do you have to use MockMvc to test REST controllers?**

No. Common alternatives:

| Library | Type | Description |
|---------|------|-------------|
| MockMvc | In-process | Tests the web layer without a real HTTP server, fast |
| RestAssured | Real HTTP | Makes real HTTP calls to a running server, readable DSL, popular for integration tests |
| TestRestTemplate | Real HTTP | Spring's built-in for full integration tests |
| WebTestClient | Real HTTP | Reactive alternative, also works for MVC |

MockMvc is good for unit-style controller tests. RestAssured is closer to real-world and commonly used in integration and contract testing.

---

**Q: `assertThat().isEqualTo()` vs `assertEquals` — which should you use?**

Both work. `assertEquals` is JUnit 5 style, common in enterprise/corporate codebases. `assertThat().isEqualTo()` is AssertJ style, more common in newer Spring Boot projects — Spring Boot includes AssertJ by default. AssertJ gives richer failure messages and chains naturally for complex assertions. The most important thing is consistency within a project — don't mix both styles.

---

## Logging

**Q: Why use `log.info` for write operations but `log.debug` for reads?**

Write operations (create, update, delete) change system state, so you want a permanent audit trail in production logs. Read operations are frequent and read-only — logging every GET at INFO level would flood logs with noise. DEBUG is off by default in production but can be enabled temporarily for troubleshooting.

Log level guide:
- `ERROR` — something broke, needs immediate attention
- `WARN` — something unexpected but handled (e.g. not found, duplicate)
- `INFO` — normal business events worth tracking (state changes)
- `DEBUG` — detailed flow for troubleshooting, too noisy for production

---

## Database Migrations

**Q: What is Flyway and how does it compare to Liquibase?**

Both are database migration tools that version-control schema changes and apply them automatically on startup. They track which migrations have run in a history table (`flyway_schema_history` or `databasechangelog`).

| | Flyway | Liquibase |
|---|---|---|
| Migration format | Plain SQL (or Java) | XML, YAML, JSON or SQL |
| Rollback | Manual | Built-in support |
| Complexity | Simple | More powerful |
| Best for | SQL-heavy schemas | Teams needing fine-grained control |

---

## Docker / Kubernetes

**Q: How does Docker Compose compare to Kubernetes?**

Same concepts, different complexity:

| Concept | Docker Compose | Kubernetes / Minikube |
|---|---|---|
| Running unit | Container | Pod |
| Service definition | Service in `docker-compose.yml` | Deployment + Service manifest |
| Internal networking | Docker Compose network | Kubernetes cluster network |
| Service discovery | Service name | Kubernetes Service name |
| Persistent storage | Docker volume | PersistentVolume / PVC |
| Config/secrets | Environment variables | ConfigMap / Secret |
| Health check | `healthcheck` in compose | `livenessProbe` / `readinessProbe` |

Docker Compose is lighter and better for local dev. Kubernetes is for production-grade orchestration.
