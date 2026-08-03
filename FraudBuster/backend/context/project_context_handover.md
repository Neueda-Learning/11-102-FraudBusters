# Project Context Handover (Conversation Summary)

This file captures what we discussed so far about the **Transaction Monitoring & Alerts** project, especially around **features, UI, and customer requirements**.

---

## 1) Project Understanding So Far

- This is a **bank/internal operations monitoring system**, not a consumer-facing app for one individual user.
- **Customer/Evaluator context:** An ecommerce application needs a transaction monitoring layer sitting **between their payment service and payment gateway**.
- Purpose: monitor incoming transactions, detect suspicious patterns, generate alerts, and manage alert lifecycle.
- Current training scope assumes:
  - no authentication for now
  - single operator workflow

---

## 2) Confirmed Alert Lifecycle

Lifecycle remains:

`OPEN -> ACKNOWLEDGED -> INVESTIGATING -> CLOSED`

`OPEN/ACKNOWLEDGED/INVESTIGATING -> DISMISSED` (false positive path)

Additional customer clarification:
- Operator gets **10 minutes to investigate**.
- Payment can be **held for up to 10 minutes**.

### ✅ Hold Window Policy (LOCKED)
| Scenario | Action |
|---|---|
| Investigation done → Alert is **true positive** | **DECLINE** the transaction |
| Investigation done → Alert is **false positive** | **RELEASE/ALLOW** the transaction |
| **10 minutes elapsed, no decision made** | **AUTO-DECLINE** (risk-safe default) |

---

## 3) Customer Throughput + Placement Requirement

### ✅ LOCKED
- **20,000 transactions per minute** (clarified — per minute, not per second)
- That is approximately **~334 requests/second** sustained load
- System sits **between user payment request and payment gateway**

Flow:

`Customer Checkout -> Payment Service -> (Our Monitoring Layer) -> Payment Gateway`

Implications:
- Low-latency inline check required
- High availability required
- No data loss / no duplicate processing
- Hybrid design: fast inline checks + async deeper checks

---

## 4) UI Features Discussed (Priority)

### Must-have (MVP)
1. Dashboard with summary cards:
   - Open alerts
   - Acknowledged alerts
   - Transactions today
   - Alerts closed today
2. Transactions list page:
   - searchable/filterable table
   - columns: txn id, account, payee, amount, timestamp, type, alert indicator
3. Active alerts list page:
   - severity, rule, status, created time
4. Alert details panel/page:
   - alert metadata
   - related transactions
   - customer verification details: name, email, phone
   - actions: Acknowledge, Investigate, Close, Dismiss
5. Alert history page:
   - closed/dismissed alerts
   - notes and timestamps
6. Rules management page:
   - list rules
   - activate/deactivate toggle
   - edit/delete

### Nice-to-have
- charts (alerts by severity, trends)
- real-time updates (websocket/SSE)
- dedup/grouping indicators

---

## 5) MVP Product Direction Agreed

- Start with **dummy data** first (not real-time ingest initially).
- Goal of this stage:
  - lock UI/UX
  - lock API contracts
  - validate lifecycle and operator flow
- Then incrementally move to real-time ingestion and scale.

Suggested phased path discussed:
1. Dummy/static data MVP
2. API-integrated flow
3. Real-time/high-throughput architecture improvements

---

## 6) Data Model We Discussed

Core tables:
1. `transactions`
2. `rules`
3. `alerts`
4. `alert_transactions` (mapping table)

Why mapping table:
- One alert may relate to multiple transactions (especially velocity rules).

---

## 7) UI Prototype Already Created

A clickable UI prototype file is available at:
- `dashboard_mockup.html`

It includes:
- sidebar navigation
- dashboard cards/charts
- alerts list + detail
- transactions page
- rules page
- history page

---

## 8) ✅ Tech Stack (LOCKED)

| Layer | Technology |
|---|---|
| Frontend | HTML + CSS + Vanilla JS |
| Backend | Java + Spring Boot |
| Database | MySQL |
| VCS | Git |
| API Testing | Postman |

---

## 9) ✅ Team Structure (LOCKED)

**3-person team:**

| Person | Primary Responsibility |
|---|---|
| **Person 1 — Frontend** | All HTML pages, CSS styling, JS fetch/API calls to backend |
| **Person 2 — Backend** | Spring Boot REST APIs, rule engine logic, alert lifecycle, 10-min auto-decline timer |
| **Person 3 — Database** | MySQL schema design, seed data, JPA/JDBC integration with Spring |

---

## 10) Git Branching Strategy

```
main              → stable, demo-ready code only
dev               → active development merge point
feature/frontend  → Person 1's work
feature/backend   → Person 2's work
feature/database  → Person 3's work
```

---

## 11) Testing Strategy (3 Phases)

| Phase | Data Source | Tool | Goal |
|---|---|---|---|
| **Phase 1 (Now - MVP)** | Dummy seeded DB data | Browser + Postman | UI/UX + lifecycle verify |
| **Phase 2 (API ready)** | Script-generated fake txns | Python/Node script | API + rule logic verify |
| **Phase 3 (Scale test)** | k6 load generator | k6 | 20k/min throughput test |

- **Phase 1 is the current focus** — no need for real-time load yet.

---

## 12) Recommended Work Order

```
Step 1: DB schema finalize              → Person 3
Step 2: Spring Boot project setup       → Person 2 (API stubs first)
Step 3: Frontend pages structure        → Person 1 (dummy data initially)
Step 4: Backend APIs connect to real DB → Person 2 + Person 3
Step 5: Frontend connects to APIs       → Person 1 (fetch calls)
Step 6: End-to-end flow testing         → All 3
Step 7: Demo dry run                    → All 3
```

---

## 13) Key Requirement Decisions to Keep in Mind

- Keep lifecycle as originally defined.
- **10-minute hold window:** auto-decline on timeout (fail-safe default).
- Because system is in payment path, design for low latency and resiliency.
- Keep MVP simple, but architecture decisions should not block future scaling.
- No authentication in scope for current phase.
- Single operator workflow assumed.
- Persist all monitored transactions in DB (not only those with alerts) for rule evaluation, auditability, and dashboard accuracy.
- `txn_type` supports `DEBIT` and `CREDIT`, but current ecommerce monitoring rules are mostly applied on `DEBIT` flows.
- Customer verification fields for operator call flow are limited to `customer_full_name`, `customer_email`, `customer_phone` in MVP.

---

## 14) Still Open / To Be Decided Later

1. Which rules are strictly **inline blocking** vs **async**?
2. Required response-time target (e.g., p95 latency)?
3. **Fail-open vs fail-closed** if monitoring service is unavailable?
4. Exact rules for ecommerce context (velocity, amount threshold, geo, etc.)?
5. Git remote repo location — GitHub or GitLab?

---

## 15) One-Liner Summary

We are building an ecommerce transaction monitoring layer (between payment initiation and gateway) using HTML/CSS/JS frontend + Spring Boot backend + MySQL, starting with a dummy-data MVP, with a strict 10-minute investigation/hold SLA (auto-decline on timeout), managed by a 3-person team.
