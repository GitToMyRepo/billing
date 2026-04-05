# Skills & Experience

Demonstrates practical skills and real-world experience relevant to the role.
Use these as talking points for competency-based and behavioural questions.

---

## Debugging & Troubleshooting Production Issues

**Scenario: An API endpoint like `getCustomer` returns nothing in production.**

**Approach:**

1. Clarify the symptom first:
   - Is it all customers or one specific ID?
   - What is the HTTP response code — 200 with empty body, 404, 500, or timeout?
   - When did it start — was there a recent deployment?

2. Check Grafana:
   - Search logs around the time of the issue for errors, warnings, or stack traces
   - Check error rate, request rate, and latency dashboards for anomalies
   - Check DB connection pool metrics if the issue could be database-related

3. Check the deployment (ArgoCD):
   - Is the service running the expected version?
   - Did a recent sync or deployment fail or introduce a regression?
   - If a bad deployment is suspected, rollback in ArgoCD and verify

4. Trace the request flow end-to-end:
   - Start at the entry point service and follow the request downstream
   - Check each service's logs in sequence until you find where the request fails or returns unexpected data
   - Don't jump around — follow the evidence from one service to the next
   - This is especially important in microservice architectures where a failure in one service can silently affect another

5. Check the database directly:
   - Connect via DBeaver or psql and run the query manually — does the data exist?
   - Verify the service can actually reach the database

6. Fix and verify:
   - Apply the fix (rollback, hotfix deployment, data fix)
   - Confirm in Grafana that error rates drop and the endpoint returns expected results

**Key message for the interview:**
> "I start by checking Grafana for errors or anomalies, then trace the request from the entry point and follow the flow downstream, checking each service's logs in sequence until I find where it fails. That way I'm not guessing — I'm following the evidence."

---

## Vendor Software & Third-Party Integrations

Relevant to: *"Configure and maintain vendor-provided software solutions; evaluate and validate functionality introduced in new vendor releases."*

- Experience evaluating new vendor releases — validating behaviour changes, regression testing against existing functionality
- Familiar with using vendor support portals and defect-resolution processes to raise and track issues
- Experience using diagnostic tools and logs to isolate whether an issue is in own code or vendor software
- Approach: reproduce the issue in a lower environment first, gather evidence (logs, request/response payloads), then engage vendor support with clear reproduction steps
