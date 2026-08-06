# Transaction Evaluation Workflow

## 1) Main Classes Involved

- `RuleEngineServiceController`
- `CentralRuleEngineServiceImpl`
- `RuleEntityRepository`
- `TransactionEntityRepository`
- `TransactionDecisionEntityRepo`
- `AmountThresholdRuleEngineServiceImpl`
- `DailyLimitRuleEngineServiceImpl`
- `NewPayeeRuleEngineImpl`
- `VelocityCheckRuleEngineImpl`

## 2) Input -> Entity Flow

1. API receives transaction JSON.
2. JSON is mapped to `TransactionEntity` by Spring.
3. `CentralRuleEngineServiceImpl` resolves by `txnId`:
   - if exists: use existing `TransactionEntity`
   - if not exists: create new `TransactionEntity` with:
     - `monitorState = RECEIVED`
     - `finalDecision = PENDING`

## 3) Active Rule Filtering

`CentralRuleEngineServiceImpl` loads only rules that are:

- active (`isActive = true`)
- not deleted (`isDeleted = false`)

from `RuleEntityRepository`.

So the transaction is evaluated only by valid live rules.

## 4) Rule Evaluation Routing

For each active rule:

- `AMOUNT_THRESHOLD` -> `AmountThresholdRuleEngineServiceImpl`
- `DAILY_LIMIT` -> `DailyLimitRuleEngineServiceImpl`
- `NEW_PAYEE` -> `NewPayeeRuleEngineImpl`
- `VELOCITY_CHECK` -> `VelocityCheckRuleEngineImpl`

Each rule returns:

- `true` = alert condition matched
- `false` = no alert condition matched

## 5) If Rule Triggers (true path)

### Entity conversion flow

`TransactionEntity` + `RuleEntity` -> `AlertEntity` -> `AlertTransactionEntity`

### What is saved in DB and when

1. **Update `transactions`**
   - same `TransactionEntity` is saved with `monitorState = HELD`.
2. **Insert into `alerts`**
   - `AlertEntity` is created with `status = OPEN`, severity, title, description, alert code.
3. **Insert into `alert_transactions`**
   - `AlertTransactionEntity` is created for triggering transaction:
     - `relationType = TRIGGERING_TRANSACTION`.
4. **Velocity-specific additional links**
   - extra `AlertTransactionEntity` rows are inserted for previous related transactions:
     - `relationType = RELATED_TRANSACTION`.
5. **Central finalization step**
   - transaction remains `HELD` and `finalDecision = PENDING`.
   - no ALLOW row is inserted into `transaction_decisions` in this branch.

## 6) If Rule Does Not Trigger (false path)

### Per-rule save behavior

Each rule may attempt release, but with guard:

- if transaction is already `HELD` by another rule, it is not downgraded.

### Central final decision (no rule triggered at all)

When all active rules return false:

1. **Update `transactions`**
   - set `monitorState = RELEASED`
   - set `finalDecision = ALLOW`
   - set decision reason.
2. **Insert into `transaction_decisions`**
   - create `TransactionDecisionEntity` with:
     - `decision = ALLOW`
     - `decidedBy = SYSTEM`
     - reason = passed all active rules.
3. **Idempotency check before insert**
   - `existsByTransactionTxnId(txnId)` prevents duplicate decision rows.

## 7) Continuous New Transaction Evaluation

For simulation/continuous mode:

1. fetch top 100 transactions where:
   - `monitorState = RECEIVED`
   - `finalDecision = PENDING`
2. evaluate each via same central flow.

This is done by `evaluatePendingTransactionsBatch()`.

## 8) Exact State Model Used

- `MonitorState`: `RECEIVED`, `HELD`, `RELEASED`, `DECLINED`
- `FinalDecision`: `PENDING`, `ALLOW`, `DECLINE`
- `DecisionType` (`transaction_decisions`): `ALLOW`, `DECLINE`
- `AlertRelationType`: `TRIGGERING_TRANSACTION`, `RELATED_TRANSACTION`

## 9) End-to-End Save Timeline (short)

1. **First save**: new `TransactionEntity` inserted (if txnId not present).
2. **Rule trigger save(s)**:
   - update `TransactionEntity` to HELD,
   - insert `AlertEntity`,
   - insert `AlertTransactionEntity` row(s).
3. **Central final save**:
   - if any trigger -> keep `HELD/PENDING`,
   - if no trigger -> set `RELEASED/ALLOW`.
4. **Decision save**:
   - only in no-alert branch, insert `TransactionDecisionEntity (ALLOW)`.


