# Anju Accompanying Medical Appointment Operation Management System  
## Backend Architecture Design

## 1. System Architecture (Monolithic Design)

### 1.1 Architecture Style
The system is implemented as a **single Spring Boot monolithic service** with clear internal domain boundaries.  
This service exposes RESTful APIs for operational workflows across property management, appointment scheduling, financial bookkeeping, file lifecycle management, and compliance auditing.

### 1.2 Runtime Topology (Offline Docker)
- `app` container: Spring Boot API service
- `mysql` container: business data persistence
- `nacos` container: local-only configuration and service registration

All services are launched with one command via `docker compose up`, without external cloud dependencies.

### 1.3 Layered Internal Structure
Within the monolith, each domain follows layered design:
- **Controller layer**: API contracts, request validation, response mapping
- **Service layer**: business rules, orchestration, state transitions
- **Repository layer**: data access and query optimization
- **Common/Config/Security/Audit**: shared cross-cutting capabilities

This keeps domain logic testable and avoids coupling transport details to business rules.

### 1.4 Module Interaction Flow
The platform follows a request-driven orchestration flow with transactional boundaries at service level:

1. **API entry and security gate**
   - Request enters Controller
   - JWT/session identity is verified
   - RBAC permission and sensitive-operation secondary password checks are applied
2. **Domain orchestration**
   - Service layer validates business constraints and idempotency keys
   - Appointment, property, finance, and file modules interact through internal service interfaces
3. **Persistence and consistency**
   - Repository operations execute inside transaction scope
   - Conflict-sensitive operations apply lock strategy before commit
4. **Audit and observability**
   - Audit aspect captures operator, action type, before/after key fields
   - Structured logs and business events are persisted for traceability
5. **Response standardization**
   - Unified response envelope and error model returned to client

Cross-domain examples:
- **Appointment creation**: Auth -> Appointment validation/conflict check -> Appointment persist -> Finance pre-bookkeeping record (if required) -> Audit write
- **Property approval**: Auth + secondary password -> Compliance check -> Property status transition -> Audit write
- **Refund operation**: Auth + finance role -> Refund policy validation -> Transaction/settlement update -> Audit write

---

## 2. Module Breakdown

## 2.1 Property Domain
Responsibilities:
- Property create/edit/list/listing status updates
- Rental rule configuration (rent, deposit, rental periods, vacancy periods)
- Materials association (images/videos metadata)
- Compliance field checks and review workflow

Core concerns:
- Strong field validation for required compliance attributes
- Status-driven lifecycle (draft, submitted, approved, invalidated)
- Query patterns optimized by status and date range

## 2.2 Appointment Domain
Responsibilities:
- Time slot management and service duration standards (15/30/60/90 minutes)
- Scheduling with conflict detection
- Rescheduling and cancellation policy enforcement
- State-machine-driven appointment lifecycle
- Auto-release if unconfirmed after 15 minutes

Key rules:
- Same staff cannot be assigned overlapping slots
- Same resource cannot be double-booked
- Max 2 reschedules per order
- Cancellation penalty capped at min(10% order amount, 50 RMB)

### Appointment State Machine (Enterprise Rule Model)
Canonical states:
- `CREATED`: appointment order created, initial validation passed
- `PENDING_CONFIRMATION`: awaiting staff/resource confirmation
- `CONFIRMED`: confirmation completed, slot locked
- `IN_SERVICE`: service has started
- `COMPLETED`: service completed and closed
- `CANCELLED`: cancelled by policy-allowed action
- `EXPIRED_RELEASED`: auto-released because confirmation timeout exceeded

Allowed transitions:
- `CREATED -> PENDING_CONFIRMATION`
- `PENDING_CONFIRMATION -> CONFIRMED`
- `CONFIRMED -> IN_SERVICE -> COMPLETED`
- `CREATED|PENDING_CONFIRMATION|CONFIRMED -> CANCELLED` (policy + authorization checks)
- `PENDING_CONFIRMATION -> EXPIRED_RELEASED` (15-minute scheduler timeout)

Transition guards:
- Reschedule allowed only when `reschedule_count < 2`
- Reschedule/cancellation request must be submitted at least 24 hours before start time
- Cancellation penalty = `min(order_amount * 0.10, 50.00 RMB)`
- Transition attempts from invalid source states are rejected with standardized business error codes

State management implementation:
- Explicit transition map in service layer (no implicit status overwrite)
- Transactional update with status version check for concurrency safety
- Audit log persisted for each accepted transition

### Conflict Detection Strategy
Conflict checks are enforced in two stages to meet enterprise consistency requirements:

1. **Pre-check stage (application level)**
   - Validate requested interval against existing active appointments for:
     - same staff ID
     - same service resource ID
   - Overlap rule: `new_start < existing_end AND new_end > existing_start`
   - Fast-fail response for obvious conflicts

2. **Commit stage (database-consistent lock stage)**
   - Re-check conflicts inside transaction with lock (`SELECT ... FOR UPDATE` or equivalent)
   - Lock scoped rows for same staff/resource and target time window
   - Commit only if no overlap remains

Supporting data strategy:
- Composite indexes for conflict queries:
  - `(staff_id, appointment_start_time, appointment_end_time, status)`
  - `(resource_id, appointment_start_time, appointment_end_time, status)`
- Active statuses included in conflict filter (`CREATED`, `PENDING_CONFIRMATION`, `CONFIRMED`, `IN_SERVICE`)
- Cancelled/expired/completed appointments excluded from conflict set

## 2.3 Financial Domain
Responsibilities:
- Multi-channel transaction bookkeeping (internal records only)
- Refund processing (original route/non-original route tracking)
- Settlement record management
- Daily reconciliation statement generation
- Exception tagging and export support
- Invoice request and issuance state tracking

Design notes:
- No external payment gateway integration in this phase
- Strict idempotency keys for transaction/refund creation
- Immutable financial event history for auditability

## 2.4 File Domain
Responsibilities:
- Chunked upload with resumable protocol
- Content-hash-based deduplication (instant upload path)
- Concurrency and bandwidth throttling
- Preview metadata for common file types
- Multi-version file rollback
- Recycle bin retention policy (30 days)

Design notes:
- File metadata stored in MySQL
- Physical binary storage abstracted behind storage service interface
- Hash uniqueness and chunk integrity checks ensure consistency

## 2.5 Auth & RBAC
Responsibilities:
- Local username/password authentication
- Password policy enforcement (min 8 chars, letters + numbers)
- Secure password hashing and verification
- Role-based access control with least privilege
- Secondary password verification for sensitive operations

Authorization model:
- Permission checks at endpoint and service boundary
- Sensitive operation guard (approval, financial reversal, destructive actions)
- Role examples: operator, reviewer, dispatcher, finance, frontline-service, admin

## 2.6 Audit Module
Responsibilities:
- Full-chain operation logging for critical workflows
- Capture operator identity, timestamp, action, and key field diffs
- Audit persistence with query support by entity/time/operator

Design notes:
- Implemented using interceptor/aspect + domain event hooks
- Structured logs for both operational debugging and compliance reviews

---

## 3. Database Design Overview

## 3.1 Core Tables
- `property`  
  Unique `property_code`, status enum, rent/deposit decimals, rental period fields, compliance validation result
- `appointment`  
  Unique `appointment_no`, start/end timestamps, status enum, penalty decimal, `reschedule_count`
- `transaction` / `settlement`  
  Unique transaction/settlement identifiers, channel enum, refundable flag, settlement status
- `file_metadata`  
  Content hash unique index, chunk metadata, version number, deletion flag, expiration timestamp

## 3.2 Supporting Tables
- User/role/permission/role_permission/user_role
- Audit log and change snapshot tables
- Statement export task and invoice tracking tables
- Idempotency record table (key + operation scope + result hash)

## 3.3 Index Strategy
- Composite indexes for high-frequency filters:
  - `(status, created_at)`
  - `(status, appointment_start_time)`
  - `(operator_id, operated_at)` for audit search
- Unique indexes:
  - `property_code`
  - `appointment_no`
  - `transaction_no`
  - `content_hash`

## 3.4 Consistency and Transactions
- ACID transactions for booking, cancellation, refund, and settlement updates
- Optimistic/pessimistic locking in conflict-sensitive scheduling paths
- Idempotency constraints to prevent duplicate appointment creation and bookkeeping
- Standardized error taxonomy (validation/business/conflict/system) for deterministic client behavior

---

## 4. Key Design Decisions

## 4.1 Why Monolith
- Domain complexity is medium-high but team delivery path is faster with a single deployable unit
- Shared transaction boundaries across scheduling + financial + audit are simpler to guarantee
- Easier offline deployment, operations, debugging, and QA acceptance in containerized environments

## 4.2 Why MySQL
- Strong transactional guarantees and mature indexing strategy for OLTP workloads
- Familiar operational profile and tooling in enterprise environments
- Excellent fit for relational workflows and audit-heavy business data

## 4.3 Why Docker Offline Deployment
- Reproducible startup in clean environments (`docker compose up`)
- Eliminates host drift and undeclared local dependencies
- Matches acceptance criteria requiring one-click startup and portable runtime behavior

---

## 5. Scalability Considerations

### 5.1 Vertical and Horizontal Growth
- Stateless API container design enables horizontal scaling of app instances
- DB connection pooling and query tuning for throughput growth
- Background scheduled jobs isolated by logical worker components

### 5.2 Hotspot Management
- Appointment conflict checks optimized with proper indexes and lock strategy
- File upload pipeline supports controlled concurrency and throttling
- Bulk exports handled asynchronously with task tracking

### 5.3 Evolution Path
- Monolith modules are designed with clear boundaries to support future extraction to services if needed
- Domain events can be introduced gradually for decoupling without immediate microservice complexity

---

## 6. Security Considerations

### 6.1 Identity and Access
- Local auth only; no third-party identity dependency
- Strong password hashing (e.g., BCrypt/Argon2-compatible strategy)
- RBAC least-privilege model across all domain APIs
- Secondary password gate for sensitive business actions

### 6.2 Data Protection
- Sensitive fields encrypted at rest or masked on output
- Strict request validation with standardized error responses
- No raw stack traces exposed to clients

### 6.3 Audit and Compliance
- Immutable audit trail for key operations (login, approval, payment/refund updates, data changes)
- Log redaction to avoid credential/sensitive leakage
- Idempotency and anti-duplicate controls for financial and appointment creation flows

### 6.4 Operational Security
- Offline runtime with no private external dependencies
- Secrets provided via environment/config, never hardcoded in source
- Containerized deployment with explicit port declarations only

---

## Non-Functional Quality Targets
- **Reliability:** deterministic state transitions + transactional consistency
- **Maintainability:** layered architecture, domain-based packaging, explicit boundaries
- **Observability:** structured logs, audit trails, health endpoints, startup checks
- **Testability:** isolated service tests + API behavior tests + reproducible Docker integration environment

---

## 7. Development Phases (Short Plan)

### Phase 1: Foundation
- Project scaffolding, Docker Compose, MySQL, Nacos, base config
- Global error handling and shared response/error model

### Phase 2: Security Baseline
- Authentication (login), password hashing, RBAC
- Secondary password verification for sensitive operations

### Phase 3: Core Business Domains
- Property APIs and review lifecycle
- Appointment scheduling, conflict detection, state machine, auto-release

### Phase 4: Financial + File Domains
- Transaction/refund/statement/invoice workflows (bookkeeping only)
- File chunk upload, resume, dedup, versioning, recycle bin retention

### Phase 5: Compliance and Hardening
- Audit logging (operator, timestamp, field changes)
- Validation hardening, idempotency, index tuning, high-load checks

### Phase 6: Testing and Delivery
- Unit tests + API tests + `run_tests.sh`
- Cold-start verification using `docker compose up`
- Final docs sync (`design.md`, `api-spec.md`, `questions.md`, `README`)

---

## 8. Final Hardening Updates

- **Appointment slot module completed**: dedicated `appointment_slot` table and APIs for slot create/list/invalidate.
- **Property compliance strengthened**: review submission now executes centralized rule checks and blocks non-compliant records.
- **File throttling improved**: upload throttling now considers request burst and estimated byte throughput.
- **Preview pipeline formalized**: preview classification and preview-content capability endpoint added for document/image/audio/video handling modes.
- **Import/export mapping completed**: field mapping catalog and per-domain mapping validation endpoints added.
- **Nacos runtime validation deepened**: docker profile startup now validates configured Nacos endpoint reachability.
- **Sensitive output masking expanded**: appointment and invoice sensitive fields are returned only in masked form.
