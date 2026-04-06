# Billing API

A Spring Boot REST API for customer billing management. Built with Java 21, PostgreSQL, and Docker.

## Tech Stack

- Java 21 / Spring Boot 4
- Spring Data JPA + Hibernate
- PostgreSQL 16
- Flyway (database migrations)
- Docker + Docker Compose
- JUnit 5 + Testcontainers (integration tests)
- JaCoCo (code coverage, 80% minimum)
- GitHub Actions (CI/CD)

## Architecture

```
HTTP Requests (port 8080)
        │
        ▼
┌─────────────────┐        port 5432        ┌──────────────────┐
│   billing-api   │ ──────────────────────► │ billing-postgres │
│ (Spring Boot)   │                         │  db: billingdb   │
└─────────────────┘                         └──────────────────┘
```

Both services run as Docker containers on the same internal Docker network,
so `billing-api` connects to `billing-postgres` using the service name as hostname.

## Concept Mapping: Docker Compose vs Kubernetes (Minikube)

This project uses Docker Compose for simplicity, but the concepts map directly to
what you'd use in a real-world Kubernetes environment (e.g. Minikube at work).

| Concept              | Docker Compose (this project)     | Kubernetes / Minikube (real world)         |
|----------------------|-----------------------------------|--------------------------------------------|
| Running unit         | Container                         | Pod                                        |
| Service definition   | Service in `docker-compose.yml`   | Deployment + Service manifest (YAML)       |
| Internal networking  | Docker Compose network            | Kubernetes cluster network                 |
| Service discovery    | Service name (e.g. `postgres`)    | Kubernetes Service name (e.g. `postgres`)  |
| Persistent storage   | Docker volume (`billing-pgdata`)  | PersistentVolume / PersistentVolumeClaim   |
| Config/secrets       | Environment variables in compose  | ConfigMap / Secret                         |
| Health check         | `healthcheck` in compose          | `livenessProbe` / `readinessProbe`         |
| Scale out            | `docker-compose scale`            | `kubectl scale deployment`                 |

### Real World Example (your work)

In Minikube at work, `tr-service` runs in its own pod and has its own database
`transactionreport` in the shared Postgres pod:

```
HTTP Requests
      │
      ▼
┌─────────────┐      port 5432     ┌───────────────────────────────┐
│ tr-service  │ ─────────────────► │         postgres pod          │
│    (pod)    │                    │  db: transactionreport        │
└─────────────┘                    │  db: billingdb (other svc)    │
                                   │  db: ...                      │
                                   └───────────────────────────────┘
```

Each service owns its own database inside the shared Postgres pod — same pattern
as this project, just orchestrated by Kubernetes instead of Docker Compose.

The connection string in `tr-service` would point to the Kubernetes Postgres service
name rather than `localhost`, exactly like how `billing-api` uses `postgres:5432`
when running inside Docker Compose.

## Running Locally (Development)

During development, run Postgres in Docker and the app via Maven:

```bash
# Start Postgres container only
docker-compose up -d postgres

# Run the app locally (connects to Docker Postgres via localhost:5432)
./mvnw spring-boot:run
```

The app reads connection details from `application.properties`:
- URL: `jdbc:postgresql://localhost:5432/billingdb`
- Username: `billing`
- Password: `billing123`

Flyway runs automatically on startup and creates the schema from `src/main/resources/db/migration/`.

## Running Fully in Docker

When the app is ready to deploy, both containers run together:

```bash
docker-compose up -d
```

In this mode, `billing-api` connects to `billing-postgres` internally via Docker's network
using the service name (`postgres`) instead of `localhost`.

## Database

| Detail   | Value       |
|----------|-------------|
| Host     | localhost   |
| Port     | 5432        |
| Database | billingdb   |
| Username | billing     |
| Password | billing123  |

Use DBeaver or any SQL client with the above details to inspect the database.

After first startup, Flyway creates:
- `customers` table
- `invoices` table
- `payments` table
- `flyway_schema_history` table (migration tracking)

## Running Tests

```bash
# Unit tests only
./mvnw test

# Full build with integration tests and coverage report
./mvnw verify
```

Coverage report is generated at `target/site/jacoco/index.html`.
Build fails if line coverage drops below 80%.

## API Endpoints

| Method | Path                        | Description          |
|--------|-----------------------------|----------------------|
| GET    | /api/customers              | List all customers   |
| POST   | /api/customers              | Create customer      |
| GET    | /api/customers/{id}         | Get customer by ID   |
| PUT    | /api/customers/{id}         | Update customer      |
| DELETE | /api/customers/{id}         | Delete customer      |
| GET    | /api/invoices               | List all invoices    |
| POST   | /api/invoices               | Create invoice       |
| GET    | /api/invoices/{id}          | Get invoice by ID    |
| POST   | /api/payments               | Record a payment     |

## Health Check

```
GET http://localhost:8080/actuator/health
```

## Known Issues / Notes

### RestAssured compatibility with Spring Boot 4
`CustomerControllerRestAssuredTest` is disabled. `rest-assured:spring-mock-mvc` 5.4.0 is compiled against Spring Test 5/6 API. Spring Boot 4 uses Spring Framework 7 which changed `MockHttpServletRequestBuilder.header()`, causing a `NoSuchMethodError` at runtime. The test logic and DSL style are correct — this will work once RestAssured releases a Spring Boot 4 compatible version. In Spring Boot 2/3 projects it works without issues.

## SonarCloud Setup

SonarCloud provides code quality and coverage analysis. The free plan covers public repos and analyses the main branch.

### First-time setup

1. Go to [sonarcloud.io](https://sonarcloud.io) and sign in with GitHub
   - If your account was created via GitLab, you need a separate GitHub-linked account
   - Install the SonarCloud GitHub App via [github.com/marketplace/sonarcloud](https://github.com/marketplace/sonarcloud) (free/$0 plan)

2. Create the project in SonarCloud:
   - Click "+" → "Analyze new project"
   - Select your organisation (e.g. `GitToMyRepo`)

   ![Analyze projects - select organisation](docs/images/sonarcloud-analyze-projects.png)

   - If the repo doesn't appear, click "create one manually"
   - Set Display Name: `billing`, Project Key: `gittomyrepo_billing`, Visibility: Public
   - Choose "With other CI tools" as the analysis method

   ![Project onboarding - Maven configuration](docs/images/sonarcloud-project-onboarding.png)

   - Copy the `SONAR_TOKEN` value shown on screen

3. Add the token to GitHub:
   - Go to your repo → Settings → Secrets and variables → Actions
   - Add secret: Name = `SONAR_TOKEN`, Value = token from SonarCloud

4. The `pom.xml` and `ci.yml` are already configured — push to main to trigger the first analysis

   `pom.xml` — add Sonar properties and plugin:
   ```xml
   <properties>
       <sonar.organization>gittomyrepo</sonar.organization>
       <sonar.projectKey>gittomyrepo_billing</sonar.projectKey>
       <sonar.host.url>https://sonarcloud.io</sonar.host.url>
   </properties>

   <plugin>
       <groupId>org.sonarsource.scanner.maven</groupId>
       <artifactId>sonar-maven-plugin</artifactId>
       <version>5.1.0.4751</version>
   </plugin>
   ```

   `.github/workflows/ci.yml` — add Sonar analysis step after tests:
   ```yaml
   - name: Analyse with SonarCloud
     env:
       SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
     run: ./mvnw verify sonar:sonar -Dsonar.projectKey=gittomyrepo_billing
   ```

### Viewing results

- Go to [sonarcloud.io](https://sonarcloud.io) → your project → Overview
- Results appear after the pipeline runs on main

   ![SonarCloud PR summary](docs/images/sonarcloud-pr-summary.png)

- Feature branch analysis requires the paid plan — free plan shows "Not analyzed" on PRs
- In your work, SonarQube is self-hosted so all branches are analysed

## Branch Protection

The `main` branch is protected with the following rules:
- All changes must go through a pull request — no direct pushes
- CI pipeline (`Build, Test & Coverage`) must pass before merging
- Rules apply to everyone including admins

To contribute: create a feature branch → push → raise PR → pipeline must be green → merge.
