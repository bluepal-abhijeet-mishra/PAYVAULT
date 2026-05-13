# PAYVAULT
PAYVAULT Real-Time Payment Processing &amp; Fraud Detection Platform

## Project Understanding

The PRD defines PayVault as a real-time payment processing and fraud detection platform built around Kafka event pipelines. Phase 1 centers on payment ingestion, enrichment, fraud scoring, decisioning, and mandatory controls such as idempotency, auditability, and RBAC.

The current repository contains the `payment-simulator` module, which is the Phase 1 producer harness mentioned in the PRD assumptions. Its purpose is to generate canonical `payments.raw` events and targeted fraud patterns so the downstream PayVault platform can be tested before real gateway integrations exist.

## Simulator Scope

The simulator now covers these PRD-aligned responsibilities:

- Publish clean card, UPI, and wallet-style payment events with the canonical schema fields.
- Inject fraud scenarios for the six rule-based checks in the PRD: velocity spike, high-value spike, geo anomaly, blacklisted merchant, blacklisted device, and odd-hours activity.
- Run configurable background traffic and explicit load batches for throughput testing.
- Expose HTTP control endpoints so QA, backend, and data teams can trigger repeatable scenarios.

## Simulator API

- `GET /api/simulator/status`
- `POST /api/simulator/payments`
- `POST /api/simulator/traffic/normal?count=25`
- `POST /api/simulator/fraud`
- `POST /api/simulator/load`
- `GET /api/simulator/profiles`
- `GET /api/simulator/scenarios`
- `GET /api/simulator/scenarios/coverage`
- `POST /api/simulator/scenarios/{scenarioId}/execute`

Example fraud request:

```json
{
  "userId": "USER_12345678",
  "scenario": "VELOCITY_SPIKE"
}
```

Example load request:

```json
{
  "totalTransactions": 10000,
  "concurrency": 64
}
```

## Professional Handoff Notes

The simulator is now structured as a reusable team harness rather than a one-off producer:

- Scenario inventory is externalised in `payment-simulator/src/main/resources/scenario-library.json`
- Scenario execution is profile-driven and category-based
- Synthetic profiles map to the simulator PRD data-generation strategy
- Representative scenarios exist across all 8 simulator PRD categories
- JUnit tests verify scenario library loading and execution routing
