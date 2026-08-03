# Backend Parallel TODO Plan (3 Dev Team)

## 1) Goal

Is file ka purpose hai ki 3 backend devs parallel me kaam start kar saken, branches clear hon, endpoints ownership fixed ho, aur integration smooth rahe.

Target flow:
1. Transaction evaluate hoti hai.
2. Rule trigger hone par alert create hota hai.
3. Operator alert status/actions handle karta hai.
4. Final ALLOW/DECLINE decision record hota hai.
5. Transaction table me decision mirror update hota hai.

---

## 2) Parallel Working Rules (Must Follow)

- Public API identifiers me `alertCode` and `txnId` use karo (DB `id` internal rakho).
- Har status change pe `alerts.status` + `alert_status_history` dono update hon.
- Final decision (`ALLOW`/`DECLINE`) pe `transaction_decisions` row create karo and `transactions` mirror update karo.
- `CLOSED` and `DISMISSED` pe meaningful reason mandatory rakho.
- PR chhoti rakho: 1 endpoint/feature per PR where possible.
- Daily sync: 15 min, blockers + contract mismatch immediate discuss.

---

## 3) Initial Branch Plan

Base branch: `develop`

- Dev A: `feature/dev-a-read-apis`
- Dev B: `feature/dev-b-alert-lifecycle`
- Dev C: `feature/dev-c-evaluation-decision`
- Optional shared hotfix branch for contract updates: `feature/shared-api-contract`

Branch rules:
- Har dev apni branch se hi PR raise kare.
- Direct push to `develop` avoid karo.
- Contract-related changes pe 3/3 review required.

---

## 4) Endpoint Ownership Split (Initial)

## Dev A - Read APIs Owner

### Endpoints
- `GET /api/dashboard/summary`
- `GET /api/alerts`
- `GET /api/alerts/{alertCode}`
- `GET /api/transactions`
- `GET /api/transactions/{txnId}`
- `GET /api/rules`
- `GET /api/alerts/{alertCode}/history`

### Scope Notes
- List APIs me filter/sort/pagination support align karo (status, severity, date, search).
- Alert detail me related transactions and latest lifecycle context include karo.

### Acceptance Criteria
- UI ko required cards/lists data mil jaye without extra joins at frontend.
- `alertCode` and `txnId` based fetch stable ho.
- Null-safe response structure (missing optional fields break na kare).

---

## Dev B - Alert Lifecycle + Actions Owner

### Endpoints
- `POST /api/alerts/{alertCode}/acknowledge`
- `POST /api/alerts/{alertCode}/investigate`
- `POST /api/alerts/{alertCode}/close`
- `POST /api/alerts/{alertCode}/dismiss`
- `POST /api/alerts/{alertCode}/actions`

### Scope Notes
- Allowed transitions enforce karo (invalid transition reject).
- Action diary (`alert_actions`) and status history alag concerns hain; dono correctly write hone chahiye.

### Acceptance Criteria
- Har valid transition pe `alert_status_history` row create ho.
- Invalid transitions pe clear validation error aaye.
- `close/dismiss` without reason reject ho.

---

## Dev C - Evaluate + Decision + Timeout Owner

### Endpoints
- `POST /api/transactions/evaluate`
- `POST /api/transactions/{txnId}/allow`
- `POST /api/transactions/{txnId}/decline`

### Background Flow
- Hold timeout job (10 min expiry handling using `hold_expires_at`).

### Scope Notes
- Rule evaluation me active + non-deleted rules only.
- Decision create hone par:
  - `transaction_decisions` insert
  - `transactions.final_decision`, `monitor_state`, `decision_reason`, `decided_at` update

### Acceptance Criteria
- Evaluate endpoint alert create/link flow complete kare.
- Allow/decline idempotency and duplicate decision protection ho.
- Timeout flow race-safe ho (manual decision vs scheduler conflict handle).

---

## 5) Contract Freeze (Day 1 Mandatory)

Freeze these before deep coding:
- Enums:
  - Alert Status: `OPEN`, `ACKNOWLEDGED`, `INVESTIGATING`, `CLOSED`, `DISMISSED`
  - Monitor State: `RECEIVED`, `HELD`, `RELEASED`, `DECLINED`
  - Final Decision: `PENDING`, `ALLOW`, `DECLINE`
  - Action Type: `NOTE_ADDED`, `CUSTOMER_VERIFIED`, `TRANSACTION_RELEASED`, `TRANSACTION_DECLINED`, `ESCALATED`
- Common response envelope (success, message, data, error)
- Date-time format (ISO-8601)
- Pagination contract (`page`, `size`, `sort`)
- Validation error format

---

## 6) First 7 Days Execution Plan

## Day 1
- Contract freeze meeting
- Endpoint payload samples lock
- DB constraints and transition matrix agree

## Day 2
- Dev A: list/detail read stubs + query layer start
- Dev B: lifecycle transition validator + status history write path
- Dev C: evaluate service skeleton + rule loader

## Day 3
- Dev A: dashboard summary + alerts list filters
- Dev B: acknowledge/investigate endpoints complete
- Dev C: allow/decline endpoints first version

## Day 4
- Dev A: alert detail + history endpoint complete
- Dev B: close/dismiss + reason validation complete
- Dev C: decision mirror updates + duplicate protection

## Day 5
- Dev C: hold timeout scheduler complete
- All: integration test for end-to-end flow
- Fix payload mismatch + enum mismatch

## Day 6
- Hardening: error handling, edge cases, logs
- API docs update
- PR cleanup

## Day 7
- Joint regression pass
- Merge to `develop`
- Release candidate tag for backend MVP

---

## 7) Integration Checkpoints

- CP1: Contract freeze signed by all (Day 1)
- CP2: Read APIs merge (Day 3 end)
- CP3: Lifecycle/actions merge (Day 4 end)
- CP4: Evaluate/decision/timeout merge (Day 5 end)
- CP5: Full workflow demo (Day 7)

---

## 8) PR Strategy

- PR size: ideally < 400 lines changed (excluding generated code).
- Naming:
  - `feat(api): alerts list + filters`
  - `feat(workflow): acknowledge/investigate transitions`
  - `feat(decision): allow-decline mirror update`
- Required reviewers:
  - Domain owner + 1 cross-owner reviewer
- Merge condition:
  - Build green
  - Basic endpoint tests passed
  - Contract unchanged or approved by all

---

## 9) Risk List + Mitigation

1. Status/decision race conditions
- Mitigation: transactional updates + guard checks + clear conflict response.

2. Duplicate decision records
- Mitigation: idempotency check before insert; unique business rule in service layer.

3. Alert and history out-of-sync
- Mitigation: same transaction boundary for status + history write.

4. Contract drift between branches
- Mitigation: Day 1 freeze + shared DTO package + CP2 payload verification.

5. Timeout vs manual operator action conflict
- Mitigation: timeout job should re-check current state before auto action.

---

## 10) Done Checklist (Owner-wise)

## Dev A Done When
- [ ] Dashboard summary endpoint working with expected counts.
- [ ] Alerts list/detail/history endpoints stable.
- [ ] Transactions list/detail + rules list available.
- [ ] Filters/pagination tested.

## Dev B Done When
- [ ] All lifecycle endpoints functional with valid transitions only.
- [ ] Every status change recorded in `alert_status_history`.
- [ ] `close/dismiss` reason validation enforced.
- [ ] Actions endpoint writes `alert_actions` correctly.

## Dev C Done When
- [ ] Evaluate endpoint creates alert/link as required.
- [ ] Allow/decline creates `transaction_decisions` row.
- [ ] Transaction mirror fields updated correctly.
- [ ] Timeout scheduler handles expiry without corrupting final state.

---

## 11) Quick Kickoff (Today)

1. 45-min contract freeze call.
2. Branches create and owner-wise endpoint markdown share.
3. Day 2 morning tak skeleton PR open karo.
4. Day 3 end tak first integration checkpoint hit karo.

This plan can be used as active working agreement for current sprint.
