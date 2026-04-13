# Business Ambiguities Log (questions.md)

Focused on appointment rules, financial logic, and file retention.

---

## Appointment Rules

## 1) Timezone for Rule Evaluation
**Question:** Which timezone is authoritative for 24-hour cancellation/reschedule checks and auto-release timing?  
**Assumption:** Business timezone is `Asia/Shanghai`; persistence can remain UTC.  
**Solution:** Normalize all API times to UTC in storage and evaluate policy windows using `Asia/Shanghai` conversion in service logic.

## 2) Conflict Detection Timing
**Question:** Is conflict checking only before insert enough under concurrent requests?  
**Assumption:** No; conflict must be rechecked within transaction lock.  
**Solution:** Use two-stage conflict detection: pre-check + DB lock-time recheck to prevent double booking of staff/resource.

## 3) Boundary Overlap Definition
**Question:** If booking A ends exactly when booking B starts, is that a conflict?  
**Assumption:** No conflict at exact boundary.  
**Solution:** Enforce overlap formula `newStart < existingEnd && newEnd > existingStart`.

## 4) Reschedule Count Policy
**Question:** Do failed reschedule attempts count toward max 2 reschedules?  
**Assumption:** Only successful committed reschedules count.  
**Solution:** Increment `rescheduleCount` only after successful transaction commit.

## 5) Auto Release Behavior
**Question:** Should unconfirmed appointments move to `CANCELLED` or a dedicated expired status?  
**Assumption:** Use a dedicated expired status to preserve auditability.  
**Solution:** Scheduled job transitions `PENDING_CONFIRMATION` appointments past 15 minutes to `EXPIRED_RELEASED` and records audit event `APPOINTMENT_AUTO_RELEASE`.

## 6) Cancellation Penalty Precision
**Question:** How should penalty be calculated and rounded (`<=10% or 50 RMB`)?  
**Assumption:** `min(orderAmount * 0.10, 50.00)` with `HALF_UP` scale 2.  
**Solution:** Centralize penalty computation in service method and reuse in all cancellation paths.

---

## Financial Logic

## 7) Refund Route Governance
**Question:** When is non-original route refund allowed?  
**Assumption:** Allowed only with explicit reason and finance authorization (plus secondary password for sensitive approval).  
**Solution:** Validate `refundMode`; require non-empty reason for `NON_ORIGINAL_ROUTE`; enforce RBAC + secondary verification.

## 8) Refund Amount Boundary
**Question:** Can cumulative refunds exceed transaction amount in split-refund scenarios?  
**Assumption:** Never allowed.  
**Solution:** Validate `existingRefund + currentRefund <= transaction.amount`; reject overflow with business error.

## 9) Statement Regeneration Policy
**Question:** Can daily statement be regenerated for same date after creation?  
**Assumption:** Not in v1 to keep reconciliation immutable.  
**Solution:** Enforce unique statement date and reject duplicate generation requests.

## 10) Invoice Lifecycle
**Question:** What minimum invoice states are required in v1?  
**Assumption:** `REQUESTED -> ISSUED` only.  
**Solution:** Provide APIs for request and issue; reject issuing if current state is not `REQUESTED`.

## 11) Idempotency Scope
**Question:** Which financial operations require idempotency to avoid duplicate bookkeeping?  
**Assumption:** Transaction creation and refund creation must be idempotent.  
**Solution:** Require `Idempotency-Key` for these endpoints and persist request fingerprint for replay-safe responses.

---

## File Retention

## 12) 30-Day Retention Trigger
**Question:** Does retention start from delete request time or end-of-day batch mark?  
**Assumption:** Retention starts at exact delete timestamp (`deleteAt + 30 days`).  
**Solution:** Set `delete_expire_at` at delete time; purge job removes records where `delete_expire_at <= now`.

## 13) Restore After Expiry
**Question:** Can a file be restored once retention period is over but purge job hasn’t executed yet?  
**Assumption:** No restoration allowed after expiry timestamp.  
**Solution:** Restore endpoint must validate `now < delete_expire_at` before clearing recycle-bin flags.

## 14) Physical Deletion Safety
**Question:** Can physical blobs be deleted immediately when metadata expires?  
**Assumption:** Only if no active version/reference points to the same content hash.  
**Solution:** Purge checks reference count/version links before deleting binary objects to avoid accidental data loss.

## 15) Dedup + Retention Interaction
**Question:** If two files share same hash and one enters recycle bin, should binary be retained?  
**Assumption:** Yes, while at least one active metadata/version references that hash.  
**Solution:** Use reference-based purge policy: delete binary only when hash reference count reaches zero.

---

## Additional Previously Captured Questions (Retained)

## 16) Property Listing Effective Time
**Question:** Is listing/delisting immediate, or can it be scheduled?  
**Assumption:** Immediate in v1.  
**Solution:** Keep immediate status-transition endpoints; no delayed scheduler in current release.

## 17) Compliance Field Baseline
**Question:** Which compliance fields are mandatory across all properties?  
**Assumption:** License number, owner contact, rental period, and financial terms are baseline mandatory fields.  
**Solution:** Maintain configurable validator with default mandatory set and rejection reason storage.

## 18) Material Attachment Limits
**Question:** What are allowed file types and max attachment count per property?  
**Assumption:** Images/videos supported, capped by configuration (default 20).  
**Solution:** Enforce MIME whitelist and per-property attachment cap in service layer.

## 19) Sensitive Data Protection Boundary
**Question:** Which fields must be encrypted-at-rest versus masked-on-output?  
**Assumption:** ID numbers, phone, and contact details need encryption-at-rest and masking by default in API output.  
**Solution:** Apply field-level encryption in persistence and role-based unmask policy for privileged audit endpoints.

## 20) Secondary Password Coverage
**Question:** Which sensitive endpoints must require secondary password verification?  
**Assumption:** Refund approval, property invalidation, manual purge, and role/permission critical operations.  
**Solution:** Enforce `X-Secondary-Token` validation interceptor and reject missing/expired tokens.

## 21) Audit Minimum Payload
**Question:** What minimum audit fields are required for compliance traceability?  
**Assumption:** Operator, role, action, entity type/id, timestamp, requestId, and key before/after field changes.  
**Solution:** Standardize audit schema and force logging on all mutation endpoints.

## 22) Import/Export Failure Strategy
**Question:** Should one invalid row fail full import, or allow partial success?  
**Assumption:** Partial success with row-level error reporting.  
**Solution:** Return batch summary (`successCount`, `failureCount`) and row error details.

## 23) Internal API Isolation
**Question:** How are internal scheduler/export APIs protected from external misuse?  
**Assumption:** Internal APIs must not be callable by normal external roles.  
**Solution:** Keep `/internal` namespace, dedicated service role, network restrictions, and mandatory audit records.

## 24) Nacos Startup Dependency Policy
**Question:** If Nacos is unavailable, should the app start with defaults or fail fast?  
**Assumption:** Fail fast in production.  
**Solution:** Block readiness until required Nacos configuration is loaded.
