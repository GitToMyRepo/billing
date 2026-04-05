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

---

## Testing Types

**Q: What are the differences between unit, integration, functional, E2E, mutation and pen tests?**

| Type | What it tests | Speed | Example |
|------|--------------|-------|---------|
| Unit | Single class/method in isolation, all dependencies mocked | Very fast | `CustomerServiceTest` with Mockito |
| Integration | Multiple components working together with real dependencies | Medium | `BillingApplicationTests` with Testcontainers + real Postgres |
| Functional / Acceptance | System behaviour against business/acceptance criteria | Medium-slow | QA testing a Jira ticket in QA environment |
| End-to-End (E2E) | Full system flow across all services from user perspective | Slow | Create customer → create invoice → pay → verify status is PAID |
| Mutation | Introduces bugs into code automatically and checks if tests catch them | Slow | PIT (Pitest) for Java |
| Penetration (Pen) | Security testing — exploiting vulnerabilities like SQL injection, auth bypass | Varies | Done by security team or external firm |

Testing pyramid — more unit tests, fewer E2E tests:
```
      /  E2E  \        ← few, slow, expensive
     / Functional\
    / Integration  \
   /   Unit Tests    \ ← many, fast, cheap
```

---

**Q: What type of testing does QA perform when they pick up a Jira ticket?**

Functional testing (also called acceptance testing) — they verify the system behaves according to the acceptance criteria on the ticket. They don't care about code internals, just "does it do what it's supposed to do?"

Depending on the stage and who is testing:
- QA testing against acceptance criteria in QA environment → functional / acceptance testing
- Business stakeholders validating in UAT environment → UAT (User Acceptance Testing)
- Automated tests that mirror acceptance criteria → automated acceptance tests (Cucumber/BDD)

Functional testing is predominantly manual in most teams, especially for complex business flows. However it can be automated using tools like Postman collections, RestAssured, Karate (API level) or Selenium/Playwright (UI level).

---

**Q: How are integration tests used in your CI/CD pipeline?**

Integration tests run at two points in the pipeline:

- Pre-merge (on PR) — prevents broken code getting into main, catches issues before they affect the team
- Post-merge — verifies the merged result is still clean, catches any merge conflicts or environment-specific issues

This gives two safety nets. Integration tests typically test the service against a real database (using Testcontainers) or downstream services in a test environment — verifying the actual wiring works, not just mocked behaviour.

> "We have unit tests that run locally and in CI, and integration tests that run pre and post merge in the pipeline. The integration tests give us confidence that the service works correctly with its real dependencies before anything reaches QA."

The modern approach is "shift left" — automate as much testing as possible earlier in the pipeline so QA can focus on exploratory testing rather than repetitive regression checks.

---

## Spring Boot Actuator

**Q: What is Spring Boot Actuator and what does `/actuator/health` return?**

Spring Boot Actuator is a dependency (`spring-boot-starter-actuator`) that automatically exposes operational endpoints for monitoring and managing the application. `/actuator/health` is the default URI — no configuration needed.

The health endpoint collects information from built-in health indicators:

| Component | What it checks |
|-----------|---------------|
| `db` | Database connection is alive — auto-detected from the datasource |
| `diskSpace` | Disk has enough free space above the configured threshold |
| `livenessState` | Is the app alive? Maps to Kubernetes liveness probe |
| `readinessState` | Is the app ready to receive traffic? Maps to Kubernetes readiness probe |
| `ping` | Simplest check — always UP if the app is running |
| `ssl` | Any configured SSL certificates aren't expiring |

You get all of this for free just by adding the Actuator dependency. The detail level is controlled by:
```properties
management.endpoint.health.show-details=always
```
Without this you'd just see `{"status": "UP"}`.

---

**Q: Can you change the `/actuator` base path?**

Yes, it is configurable:

```properties
# Change base path - health moves to /management/health
management.endpoints.web.base-path=/management

# Move management to a different port (common in production)
management.server.port=8081
```

Moving management endpoints to a separate port is a security best practice — you don't want `/actuator` publicly accessible, only reachable internally by Kubernetes or monitoring tools like Grafana.

In Kubernetes, `livenessState` and `readinessState` map directly to pod liveness and readiness probes. If `readinessState` goes DOWN, Kubernetes stops sending traffic to that pod.

---

**Q: Is Flyway's `flyway_schema_history` table the same as Liquibase's changelog table?**

Yes, same concept — both track which migrations have already been applied so they don't run again on the next startup.

| | Flyway | Liquibase |
|---|---|---|
| Table name | `flyway_schema_history` | `databasechangelog` |
| Tracks | Each versioned migration file | Each changeset |
| Key columns | `version`, `script`, `checksum`, `success` | `id`, `author`, `filename`, `exectype` |

Both tools check this table on startup — if a migration is already recorded, it skips it. If it's new, it runs it and records it.

One difference: Liquibase also has a `databasechangeloglock` table that prevents two instances running migrations simultaneously. Flyway handles this with a database-level lock instead.

---

**Q: Why did Flyway not run in Spring Boot 4 with just `flyway-core` on the classpath?**

Spring Boot 4 split its auto-configuration into smaller modules. Unlike Spring Boot 3 where `flyway-core` alone was enough to trigger auto-configuration, Spring Boot 4 requires the dedicated starter:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
```

Without this starter, Flyway is on the classpath but the auto-configuration bean that triggers migrations on startup is never registered, so migrations silently don't run.

---

## REST Principles

**Q: What is REST and what are its key principles?**

REST (Representational State Transfer) is an architectural style for designing APIs. Key principles:

- **Resource-based URLs** — URLs represent resources (nouns), not actions. `/api/customers` not `/api/getCustomers`
- **HTTP methods define the action** — the same URL does different things depending on the method (GET, POST, PUT, DELETE)
- **Stateless** — each request contains all the information needed, server holds no client session state
- **Uniform interface** — consistent conventions across all endpoints

RPC style (anti-pattern):
```
POST /api/createCustomer
GET  /api/getCustomers
POST /api/deleteCustomer
```

REST style (correct):
```
POST   /api/customers
GET    /api/customers
DELETE /api/customers/{id}
```

---

**Q: What is the difference between GET and POST?**

| | GET | POST |
|---|---|---|
| Purpose | Retrieve data | Create/submit data |
| Data location | URL query string | Request body |
| Idempotent | Yes — same result every time | No — calling twice creates two records |
| Cacheable | Yes — browsers and proxies can cache | No |
| Side effects | None — should never change state | Yes — changes server state |
| Security | Parameters visible in URL, logs, browser history | Body is not logged by default |

Key point for interviews: sensitive data (passwords, tokens) must never go in a GET URL because URLs are logged in server logs, browser history, and proxy logs. Always use POST body for sensitive data.

**Q: What does idempotent mean in the context of HTTP methods?**

An operation is idempotent if calling it multiple times produces the same result as calling it once.

- GET — idempotent (reading data doesn't change it)
- PUT — idempotent (replacing a resource with the same data twice has the same result)
- DELETE — idempotent (deleting something that's already deleted still results in it being deleted)
- POST — not idempotent (creating the same customer twice creates two records)
- PATCH — not idempotent (depends on implementation)

---

## CI/CD & GitHub Actions

**Q: What is pipeline as code?**

Pipeline as code means your CI/CD configuration lives in the repository alongside the code, versioned and reviewed like any other file. Each platform has its own required convention:

| Platform | File location |
|----------|--------------|
| GitHub Actions | `.github/workflows/*.yml` |
| GitLab CI | `.gitlab-ci.yml` in root |
| Jenkins | `Jenkinsfile` in root |
| Azure DevOps | `azure-pipelines.yml` in root |
| CircleCI | `.circleci/config.yml` |
| Bitbucket | `bitbucket-pipelines.yml` in root |

The alternative — configuring pipelines through a UI — is an anti-pattern because it's not reproducible, auditable, or version-controlled.

---

**Q: Explain the GitHub Actions CI pipeline in this project.**

The pipeline is defined in `.github/workflows/ci.yml` and triggers on every push and every PR targeting main.

It has two jobs that run in sequence:

Job 1 — `build-and-test`:
1. Checks out the code
2. Installs JDK 21 (Temurin) with Maven dependency caching
3. Runs unit tests (`./mvnw test`)
4. Runs full build with integration tests and JaCoCo 80% coverage check (`./mvnw verify`)
5. Uploads the JaCoCo HTML report as a downloadable artifact

Job 2 — `docker-build` (only runs if job 1 passes):
1. Builds the JAR
2. Builds the Docker image tagged with the Git commit SHA (`billing-api:abc1234`)
3. Also tags as `latest`

The commit SHA tag means every Docker image is traceable back to the exact commit that produced it.

---

**Q: Does GitHub Actions always run on Ubuntu?**

No, `runs-on` is configurable:
- `ubuntu-latest` — Linux (most common, cheapest)
- `windows-latest` — Windows
- `macos-latest` — macOS
- Self-hosted runner — your own machine or server registered with GitHub

---

**Q: What are `actions/checkout` and `actions/setup-java`?**

These are GitHub-provided reusable actions, versioned with `@v4`. They are not built-in commands — they are configurable and versioned. You reference them like dependencies. The `@v4` pins the version so a breaking change in the action doesn't unexpectedly break your pipeline.

You can also write your own custom actions or use community actions from the GitHub Marketplace.

---

**Q: What is the difference between `mvnw` and `mvn`?**

`mvnw` is the Maven Wrapper — a script included in the project that downloads and uses a specific Maven version defined in `.mvn/wrapper/maven-wrapper.properties`.

`mvn` uses whatever Maven version is installed on the machine.

In CI pipelines always use `./mvnw` because:
- The runner may not have Maven installed
- Guarantees everyone uses the exact same Maven version — local machine, colleague's machine, and CI runner all identical
- Eliminates "works on my machine" issues from different Maven versions

---

**Q: Where is the 80% code coverage threshold configured in this project?**

In `pom.xml`, not in the CI pipeline file. The JaCoCo Maven plugin has a `check` goal bound to the `verify` phase:

```xml
<execution>
    <id>check</id>
    <phase>verify</phase>
    <goals>
        <goal>check</goal>
    </goals>
    <configuration>
        <rules>
            <rule>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</execution>
```

When the pipeline runs `./mvnw verify`, Maven triggers the JaCoCo check. If coverage is below 80%, the command exits with a non-zero code which fails the pipeline step. GitHub Actions just runs the command and checks the exit code — it doesn't know anything about coverage thresholds itself.

Note: in the Python project, the coverage threshold was configured directly in `.gitlab-ci.yml` using pytest-cov's `--cov-fail-under` flag. The concept is the same — fail the build if coverage drops below the threshold — but the configuration location differs by language and tooling.

The general rule is — configure the threshold wherever the build tool lives, not in the CI pipeline file:

| Language / Build tool | Where to configure coverage threshold |
|----------------------|--------------------------------------|
| Java / Maven | `pom.xml` (JaCoCo plugin) |
| Java / Gradle | `build.gradle` (JaCoCo task) |
| Python / pytest | `pytest.ini`, `setup.cfg`, or `--cov-fail-under` CLI flag in pipeline |
| JavaScript / Jest | `jest.config.js` (`coverageThreshold`) |
| .NET | `.runsettings` file or test command arguments |

The CI pipeline just runs the build command. If the build tool enforces the threshold and fails, the pipeline fails. The CI platform (GitLab, GitHub, Jenkins) doesn't need to know about coverage thresholds at all.
