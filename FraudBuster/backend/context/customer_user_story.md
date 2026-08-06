# Customer User Story Context - FraudBusters MVP

This file is a customer-facing narrative for MVP alignment.
Its purpose is to confirm what problem we are solving, how the workflow operates, and what successful delivery looks like.

---

## 1) Problem Statement (Customer Perspective)

In an ecommerce payment journey, suspicious transactions can reach the payment gateway without structured monitoring and investigation controls.

FraudBusters introduces a monitoring layer between payment service and payment gateway that can:
- evaluate incoming transactions,
- generate fraud alerts,
- support operator-led investigation,
- enforce controlled outcomes,
- and keep a full audit trail.

System placement:

```text
Customer Checkout -> Payment Service -> FraudBusters Monitoring Layer -> Payment Gateway
```

---

## 2) Primary Personas

1. **Fraud Operations Analyst (Primary User)**
   - Reviews dashboard metrics, investigates active alerts, and closes or dismisses alerts.

2. **Risk Lead / Supervisor (Review User)**
   - Reviews alert volume, false-positive patterns, and operational quality.

3. **System (MVP Actor: SYSTEM)**
   - Evaluates rules, creates alerts, and records status and decision history.

---

## 3) Core User Story (MVP)

**As a Fraud Operations Analyst**, I want to quickly identify high-risk alerts, investigate them within the hold window, and mark them as fraud or false positive, so that risky payments are blocked, legitimate payments are released, and every action remains audit-ready.

---

## 4) Project Flow (End-to-End)

### A) Runtime Transaction Flow

1. A transaction reaches the FraudBusters monitoring layer.
2. The transaction is recorded in the database.
3. Active rules are evaluated (Amount Threshold, Velocity, New Payee, Daily Limit).
4. If a rule is triggered, an alert is created with status `OPEN`.
5. The alert is linked to one or more related transactions.
6. Initial status history is written for auditability.

### B) Operator Investigation Flow

1. The analyst opens the dashboard to view priority counts and recent open alerts.
2. The analyst takes lifecycle actions:
   - `OPEN -> ACKNOWLEDGED -> INVESTIGATING`
3. Final decision paths:
   - Confirmed fraud -> alert `CLOSED` + transaction `DECLINE`
   - False positive -> alert `DISMISSED` + transaction `ALLOW`
4. Every transition and decision is recorded in history/audit tables.

### C) Decision and Audit Flow

1. A decision row is created (`ALLOW` or `DECLINE`).
2. The transaction mirror fields are updated (`monitor_state`, `final_decision`, `decision_reason`, `decided_at`).
3. Timeline visibility is retained through status history and action logs.

---

## 5) Lifecycle and SLA Policy (Locked)

Lifecycle:

```text
OPEN -> ACKNOWLEDGED -> INVESTIGATING -> CLOSED
OPEN/ACKNOWLEDGED/INVESTIGATING -> DISMISSED
```

Hold-window policy (10 minutes):
- True positive: decline transaction.
- False positive: release/allow transaction.
- No decision within 10 minutes: auto-decline (risk-safe default).

---

## 6) Dashboard User Story Slice (Current Focus)

**As an Analyst**, I want a single dashboard that immediately shows monitoring health and recent open alerts, so I can prioritize action without switching screens.

### Dashboard API expectations
- `GET /api/dashboard/summary`
- `GET /api/dashboard/recent-alerts`

### Summary response fields
- `openAlerts`
- `acknowledgedAlerts`
- `transactionsToday`
- `closedToday`

### Recent alert list fields
- `alertCode`
- `severity`
- `ruleName`
- `createdAt`
- `status`

---

## 7) Acceptance Criteria for Customer Sign-off

1. Dashboard cards load from live database values, not static mock values.
2. Recent Open Alerts show newest-first ordering.
3. Invalid lifecycle transitions are rejected.
4. Close and dismiss actions persist a reason and appear in history.
5. Transaction decisions (`ALLOW`/`DECLINE`) are traceable in audit data.
6. End-to-end single-operator flow runs without authentication dependency (MVP scope).

---

## 8) Out of Scope for MVP

- Multi-user authentication and role-based access control.
- Real-time push updates via WebSocket/SSE.
- ML-based risk scoring.
- Advanced assignment queues/escalation routing.
- Full enterprise analytics and reporting suite.

---

## 9) Customer Demo Walkthrough

1. Show dashboard summary cards and recent open alerts.
2. Open an alert and acknowledge it.
3. Move the alert to investigating.
4. Resolve one alert as `CLOSED` and show decline impact on transaction decision.
5. Resolve another alert as `DISMISSED` and show allow/release impact.
6. Show status history and decision audit evidence.

---

## 10) Decisions Needed from Customer

1. Should `closedToday` include only `CLOSED`, or include `DISMISSED` as well?
2. In MVP, should rules be editable, toggle-only, or read-only?
3. For `transactionsToday`, should counting use business transaction time (`txn_timestamp`) or database insert time?
4. In production failure scenarios, should monitoring be fail-open or fail-closed?

---

## 11) One-Line Story for Presentation

FraudBusters MVP gives fraud teams a clear operational dashboard and controlled alert lifecycle so suspicious ecommerce transactions are investigated quickly, risky payments are blocked safely, and every outcome is audit-ready.

