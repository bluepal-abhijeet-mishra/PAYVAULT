# PayVault Payment Simulator

## Purpose

This module is the dedicated simulator for the PayVault platform.

It generates controlled payment traffic, fraud patterns, RBAC probes, and load scenarios so the PayVault platform teams can test ingestion, fraud detection, decisioning, alerts, analytics, and performance.

Simple interpretation:

- the simulator is the `input driver`
- the PayVault platform is the `system under test`

The simulator produces the test input correctly. The downstream platform proves whether the business outcome is correct.

## What It Does

- publishes canonical payment events to `payments.raw`
- generates PRD-aligned fraud scenarios
- exposes scenario execution APIs
- supports profile-driven synthetic data generation
- supports load batches for throughput testing
- contains the full 106-scenario catalog from the simulator PRD

## Key Files

- [SIMULATOR_HANDOFF.md](/D:/PAYVAULT/PAYVAULT/payment-simulator/SIMULATOR_HANDOFF.md:1)
- [scenario-library.json](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/resources/scenario-library.json:1)
- [application.yml](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/resources/application.yml:1)
- [SimulatorCommandController.java](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/java/com/payvault/simulator/controller/SimulatorCommandController.java:1)
- [ScenarioCatalogService.java](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/java/com/payvault/simulator/service/ScenarioCatalogService.java:1)
- [ScenarioExecutionService.java](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/java/com/payvault/simulator/service/ScenarioExecutionService.java:1)
- [PaymentSimulatorService.java](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/java/com/payvault/simulator/service/PaymentSimulatorService.java:1)
- [SyntheticDataGenerator.java](/D:/PAYVAULT/PAYVAULT/payment-simulator/src/main/java/com/payvault/simulator/service/SyntheticDataGenerator.java:1)

## How To Run

```bash
mvn test
mvn spring-boot:run
```

Default simulator base path:

`/api/simulator`

## Main APIs

- `GET /api/simulator/status`
- `GET /api/simulator/profiles`
- `GET /api/simulator/scenarios`
- `GET /api/simulator/scenarios/coverage`
- `POST /api/simulator/scenarios/{scenarioId}/execute`
- `POST /api/simulator/payments`
- `POST /api/simulator/traffic/normal?count=25`
- `POST /api/simulator/fraud`
- `POST /api/simulator/load`

## Integration Notes

The simulator can directly do these things:

- create test traffic
- publish Kafka events
- drive fraud patterns
- trigger some REST/RBAC probes
- submit load runs

These outcomes still depend on the PayVault platform:

- fraud score correctness
- decision correctness
- DB writes
- Redis idempotency and rate limiting
- alert routing
- dashboard updates
- audit/HMAC verification

## Team Handoff

Use [SIMULATOR_HANDOFF.md](/D:/PAYVAULT/PAYVAULT/payment-simulator/SIMULATOR_HANDOFF.md:1) as the full operational and integration document for backend, data, frontend, QA, and security teams.
