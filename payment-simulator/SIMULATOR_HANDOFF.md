# PayVault Simulator Handoff Guide

## 1. Purpose

The `payment-simulator` module is the dedicated test harness for the PayVault platform.

Its job is to generate controlled payment traffic and scenario-driven fraud/event patterns so the PayVault platform teams can:

- validate ingestion into `payments.raw`
- exercise fraud rules and decision thresholds
- test alerting, analytics, and dashboard behavior
- run RBAC and protected-endpoint probes
- generate sustained traffic for throughput and latency testing

This simulator is not the payment platform itself. It is the upstream stimulus generator and scenario orchestrator used by backend, data, frontend, QA, and security teams while the main platform is under development.

---

## 2. Scope

The simulator is aligned to the PayVault Simulator PRD dated **May 12, 2026**.

It contains the full scenario inventory across all 8 categories:

1. `Happy Path`
2. `Boundary Conditions`
3. `Negative & Failure`
4. `Alert Engine`
5. `RBAC & Security`
6. `Dashboard & Analytics`
7. `Load & Performance`
8. `End-to-End`

The current scenario library contains **106 scenarios**.

Important:

- some scenarios are fully executable inside the simulator today
- some scenarios are cataloged and callable, but their final assertions depend on downstream PayVault services being available
- this is expected, because many PRD scenarios require PostgreSQL, Redis, Kafka consumers, dashboard APIs, alert services, ML fallback, DLQ handling, and analytics stores that belong to the main platform

---

## 3. High-Level Architecture

The simulator has six main responsibilities:

### 3.1 Scenario Library

All scenarios are defined in:

`payment-simulator/src/main/resources/scenario-library.json`

This file is the primary executable catalog for the simulator. New scenarios should be added here first.

### 3.2 Synthetic Data Generator

The simulator produces repeatable, profile-based payloads using:

`com.payvault.simulator.service.SyntheticDataGenerator`

Supported synthetic profiles:

- `CLEAN_USER`
- `NEW_USER`
- `HIGH_RISK_USER`
- `BLACKLISTED_MERCHANT`
- `BLACKLISTED_DEVICE`
- `GEO_ANOMALY_PAIR`
- `VELOCITY_BURST`
- `HIGH_VALUE_SPIKE`

### 3.3 Payment Event Publisher

The simulator publishes canonical payment events to Kafka topic:

`payments.raw`

Canonical event schema:

- `transactionId`
- `userId`
- `amount`
- `currency`
- `merchantId`
- `paymentChannel`
- `location`
- `deviceId`
- `timestamp`

### 3.4 Scenario Executor

The simulator resolves a scenario ID into one of these execution types:

- direct Kafka event emission
- fraud-pattern generation
- REST/RBAC probe
- traffic batch generation
- load run submission

### 3.5 Assertion Hooks

The simulator contains assertion scaffolding for downstream validation:

- Kafka decisions listener
- PostgreSQL check hooks
- Redis check hooks
- RBAC REST checks

These hooks are present so the platform teams can gradually turn more cataloged scenarios into full end-to-end pass/fail validations.

### 3.6 Load Driver

The simulator includes a built-in load driver for transaction bursts and sustained traffic generation. It is intended to feed the platform, not to replace full observability tooling.

---

## 4. Module Structure

Key files:

- [SIMULATOR_HANDOFF.md](/D:/PAYVAULT/PAYVAULT/payment-simulator/SIMULATOR_HANDOFF.md:1)
- [pom.xml](/D:/PAYVAULT/PAYVAULT/payment-simulator/pom.xml:1)
- [application.yml](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/resources/application.yml:1)
- [scenario-library.json](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/resources/scenario-library.json:1)
- [SimulatorCommandController.java](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/java/com/payvault/simulator/controller/SimulatorCommandController.java:1)
- [PaymentSimulatorService.java](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/java/com/payvault/simulator/service/PaymentSimulatorService.java:1)
- [ScenarioCatalogService.java](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/java/com/payvault/simulator/service/ScenarioCatalogService.java:1)
- [ScenarioExecutionService.java](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/java/com/payvault/simulator/service/ScenarioExecutionService.java:1)
- [SyntheticDataGenerator.java](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/java/com/payvault/simulator/service/SyntheticDataGenerator.java:1)
- [LoadDriverService.java](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/java/com/payvault/simulator/service/LoadDriverService.java:1)
- [AssertionEngine.java](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/java/com/payvault/simulator/service/AssertionEngine.java:1)
- [PayVaultRestClient.java](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/java/com/payvault/simulator/service/PayVaultRestClient.java:1)

---

## 5. Runtime Dependencies

The simulator is a Spring Boot application and expects these integrations to be available when used against the platform:

- Kafka
  - required for publishing `payments.raw`
- PayVault backend APIs
  - required for RBAC and protected-endpoint scenarios
- PostgreSQL
  - required for DB-level assertions
- Redis
  - required for idempotency and rate-limit assertions
- downstream platform consumers
  - fraud engine
  - decision engine
  - alerting
  - analytics

Minimum local dependency to run the simulator in producer mode:

- Java 17
- Maven
- Kafka reachable at configured bootstrap servers

---

## 6. Configuration

Current simulator configuration is in:

[application.yml](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/resources/application.yml:1)

Important keys:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092

simulator:
  topic:
    payments-raw:
      name: payments.raw
  rate:
    normal-traffic-ms: 500
  traffic:
    auto-enabled: true
    min-batch-size: 10
    max-batch-size: 50
  fraud:
    chaos-probability: 0.02
```

Recommended team usage:

- set `spring.kafka.bootstrap-servers` to the shared dev/staging Kafka cluster
- keep `simulator.topic.payments-raw.name` consistent with platform topic naming
- disable auto traffic when doing deterministic scenario testing:

```yaml
simulator:
  traffic:
    auto-enabled: false
```

Use auto traffic only for passive background event generation or demo environments.

---

## 7. How To Run

### 7.1 Compile and Test

```bash
mvn test
```

### 7.2 Start the Simulator

```bash
mvn spring-boot:run
```

Default port:

`8080`

Base simulator API:

`/api/simulator`

### 7.3 Recommended Startup Order

For integrated testing:

1. Start Kafka
2. Start downstream PayVault services that consume `payments.raw`
3. Start PostgreSQL / Redis / other dependencies used by the platform
4. Start `payment-simulator`
5. Trigger scenarios from the simulator API

---

## 8. How The Team Connects To The Simulator

There are two integration styles.

### 8.1 Kafka-Driven Integration

Use this when the team wants the simulator to seed traffic directly into the event pipeline.

Flow:

1. simulator publishes canonical events to `payments.raw`
2. ingestion/enrichment/fraud/decision services consume those events
3. downstream teams observe `payments.enriched`, `payments.scored`, `payments.decisions`, `alerts.fraud`, database tables, and dashboards

This is the primary integration path.

### 8.2 REST-Driven Integration

Use this for scenarios involving:

- RBAC checks
- alert resolution
- analytics endpoint validation
- token and access-control behavior

The simulator uses `PayVaultRestClient` for these cases and assumes the main PayVault backend is running at the configured base URL.

Current base URL in code:

`http://localhost:8080/api/v1`

If the platform backend runs elsewhere, update:

[PayVaultRestClient.java](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/java/com/payvault/simulator/service/PayVaultRestClient.java:1)

---

## 9. API Reference

### 9.1 Status

`GET /api/simulator/status`

Purpose:

- returns simulator runtime status
- topic name
- whether auto traffic is enabled
- total published counters

### 9.2 Supported Profiles

`GET /api/simulator/profiles`

Purpose:

- lists the available synthetic data profiles

### 9.3 Scenario Catalog

`GET /api/simulator/scenarios`

Purpose:

- returns the loaded scenario inventory
- includes category, priority, execution mode, profile, and executability

### 9.4 Scenario Coverage

`GET /api/simulator/scenarios/coverage`

Purpose:

- reports category count
- scenario counts by priority
- executable scenario count

### 9.5 Execute a Scenario

`POST /api/simulator/scenarios/{scenarioId}/execute`

Examples:

```http
POST /api/simulator/scenarios/HP-01/execute
POST /api/simulator/scenarios/BC-07/execute
POST /api/simulator/scenarios/LP-05/execute
```

Purpose:

- resolves the scenario from the scenario library
- emits Kafka stimulus, REST probe, or load batch depending on scenario type

### 9.6 Publish a Manual Payment

`POST /api/simulator/payments`

Example:

```json
{
  "paymentChannel": "CARD",
  "profileName": "CLEAN_USER",
  "currency": "INR",
  "amount": 2000,
  "location": "Mumbai"
}
```

Purpose:

- publish a single custom payment event

### 9.7 Publish Normal Traffic

`POST /api/simulator/traffic/normal?count=25`

Purpose:

- emits a batch of clean transactions

### 9.8 Publish a Fraud Scenario

`POST /api/simulator/fraud`

Example:

```json
{
  "userId": "USER_12345678",
  "scenario": "VELOCITY_SPIKE"
}
```

Supported fraud scenario values:

- `VELOCITY_SPIKE`
- `HIGH_VALUE_SPIKE`
- `GEO_ANOMALY`
- `BLACKLISTED_MERCHANT`
- `BLACKLISTED_DEVICE`
- `ODD_HOURS`

### 9.9 Run a Load Batch

`POST /api/simulator/load`

Example:

```json
{
  "totalTransactions": 100000,
  "concurrency": 64
}
```

Purpose:

- submits a high-volume event generation batch

---

## 10. Scenario Execution Model

Each scenario in the library has:

- `scenarioId`
- `category`
- `title`
- `priority`
- `executionMode`
- `executable`
- optional fraud scenario
- optional synthetic profile
- optional payment overrides
- expected outcomes

Execution modes:

- `KAFKA`
  - emits direct payment events
- `KAFKA_ASSERTION`
  - emits scenario traffic intended for downstream validation
- `REST_ASSERTION`
  - probes PayVault HTTP endpoints
- `LOAD`
  - submits traffic batches
- `INTEGRATION`
  - cataloged scenario that requires downstream platform dependencies and richer assertion support

Interpretation:

- `executable=true` means the simulator can actively stimulate the scenario now
- `executable=false` means the scenario is intentionally cataloged but still depends on downstream platform implementation or additional assertion logic

---

## 11. Team Usage Recommendations

### 11.1 Backend Team

Use the simulator to:

- validate topic ingestion into `payments.raw`
- verify fraud rule responses against seeded patterns
- verify threshold handling
- test blacklists, odd-hours, and velocity patterns

Recommended first scenarios:

- `HP-01`
- `BC-02`
- `BC-07`
- `BC-10`
- `NF-09`
- `NF-10`
- `E2E-01`

### 11.2 Data Engineering Team

Use the simulator to:

- validate enrichment inputs
- validate downstream aggregations
- exercise dashboard and analytics scenarios with controlled event populations

Recommended scenarios:

- `HP-07`
- `DA-01`
- `DA-03`
- `DA-05`
- `DA-06`
- `DA-07`

### 11.3 Frontend Team

Use the simulator to:

- drive live transaction stream UI
- drive alert lists
- drive geo heatmap and ratio panels

Recommended scenarios:

- `DA-01`
- `AE-01`
- `AE-02`
- `DA-03`
- `DA-07`

### 11.4 Security Team

Use the simulator to:

- validate access-control probes
- validate token flows when backend endpoints are ready
- verify PII and audit constraints in integrated environments

Recommended scenarios:

- `RB-03`
- `RB-10`
- `RB-11`
- `RB-16`
- `RB-18`

### 11.5 QA Team

Use the simulator to:

- execute scenario library by category
- verify regressions against fixed scenario IDs
- coordinate with platform teams for pass/fail expectations

Recommended execution order:

1. `Happy Path`
2. `Boundary Conditions`
3. `Negative & Failure`
4. `Alert Engine`
5. `RBAC & Security`
6. `Dashboard & Analytics`
7. `End-to-End`
8. `Load & Performance`

---

## 12. Example Team Workflow

Example: backend team validating fraud rule handling.

1. Start the platform services that consume `payments.raw`
2. Disable simulator auto traffic for deterministic testing
3. Start the simulator
4. Execute:

```http
POST /api/simulator/scenarios/BC-02/execute
POST /api/simulator/scenarios/BC-07/execute
POST /api/simulator/scenarios/NF-09/execute
```

5. Observe:

- `payments.enriched`
- `payments.scored`
- `payments.decisions`
- alert topics
- decision table
- application logs

6. Compare platform output to the expected outcomes embedded in the scenario definitions

---

## 13. What The Simulator Guarantees

The simulator guarantees:

- canonical payment event generation
- profile-driven synthetic traffic
- fraud-pattern emission
- scenario inventory management
- scenario-by-scenario execution entrypoints
- load batch submission
- team-facing API for traffic generation

Simple interpretation for the team:

- the simulator is the `input driver`
- the PayVault platform is the `system under test`

The simulator does not guarantee by itself:

- actual fraud decisions
- actual DB writes
- actual Redis idempotency behavior
- actual alert delivery
- actual dashboard rendering
- actual HMAC verification

Those outcomes depend on the PayVault platform components your team is developing.

Example:

If the simulator executes scenario `BC-07`, it can successfully generate and publish the high-value transaction pattern to Kafka. That proves the simulator worked correctly.

But the following still depend on the downstream platform:

- fraud engine consumes the event
- scoring logic applies the correct rule delta
- decision engine writes the final decision
- alerts are emitted if required
- database and dashboard reflect the result

So the simulator proves the test input was produced correctly. The platform proves the business outcome was handled correctly.

---

## 14. Known Integration Dependencies

Scenarios that remain integration-dependent typically require one or more of:

- Fraud Engine implementation
- Decision Engine implementation
- alert service and notification sink
- PostgreSQL schema and write path
- Redis idempotency and rate limiting
- dashboard and analytics endpoints
- ML scoring service and circuit breaker behavior
- DLQ and replay infrastructure
- observability stack

This is normal. The simulator is already prepared to drive these scenarios once the platform modules are available.

---

## 15. Extension Guide

When adding a new scenario:

1. Add it to `scenario-library.json`
2. Choose the correct:
   - category
   - priority
   - execution mode
   - profile
   - fraud scenario or payment overrides
3. Mark:
   - `executable=true` if the simulator can actively stimulate it now
   - `executable=false` if it still depends on platform implementation
4. Add or extend tests if a new execution path is introduced

Do not hardcode new scenario definitions in Java unless the execution engine itself needs a new capability.

---

## 16. Recommended Next Integration Tasks For Platform Teams

To get maximum value from this simulator quickly, the platform teams should implement in this order:

1. consume `payments.raw`
2. expose decisions to `payments.decisions`
3. persist decisions to PostgreSQL
4. enable alert creation
5. expose analytics endpoints
6. expose RBAC-protected REST endpoints
7. enable Redis-backed idempotency and rate limits
8. enable observability and DLQ behavior

Once those are available, more scenarios can move from cataloged/integration-only to full pass/fail execution.

---

## 17. Verification Status

Current simulator verification:

- `mvn test` passes
- scenario catalog loads successfully
- catalog contains 106 scenarios
- execution routing is tested for representative paths

---

## 18. Final Notes For The Team

Treat the simulator as the single source of truth for upstream test stimulus.

Use scenario IDs in bug reports, integration notes, and QA evidence.

Recommended format:

- `Scenario: BC-07`
- `Environment: shared-dev`
- `Observed output: ...`
- `Expected outcome: ...`

This keeps communication between backend, frontend, QA, and security aligned around the same scenario language.
