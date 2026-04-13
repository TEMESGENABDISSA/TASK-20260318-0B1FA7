# Anju Accompanying Medical Appointment Operation Management System (Pure Backend)

Backend API system for offline operation management of:
- property management
- appointment scheduling
- financial bookkeeping
- file lifecycle management
- authentication/RBAC and audit logging

Tech stack:
- Java 17
- Spring Boot
- MySQL 8
- Nacos (local/offline)
- Docker Compose

---

## 1) Project Description

This project is a monolithic Spring Boot backend designed for operational closed-loop scenarios of:
- property administration
- accompanying medical appointment scheduling and state transitions
- transaction/refund bookkeeping (no external payment integration)
- chunk upload/resumable file management with dedup/versioning/recycle logic

Core engineering features include:
- role-based access control (RBAC)
- secondary password verification for sensitive actions
- audit logging with timestamp + field-level changes
- global JSON error handling

---

## 2) How to Run

### Prerequisites
- Docker Desktop (or Docker Engine + Compose plugin)

### One-command startup
From the `repo/` directory:

```bash
docker compose up --build
```

To run in background:

```bash
docker compose up -d --build
```

To stop services:

```bash
docker compose down
```

---

## 3) Service URLs

After startup, services are available at:

- Backend API: `http://localhost:8080`
- MySQL: `localhost:3306`
- Nacos Console: `http://localhost:8848/nacos`

### Default credentials / settings
- MySQL root user: `root`
- MySQL root password: `root`
- App DB name: `anju_ops`

> Note: This project is designed for local/offline deployment with no private external dependencies.

---

## 4) Test Instructions

### Run all tests (recommended)
From `repo/`:

```bash
sh ./run_tests.sh
```

The script will:
- run unit tests
- run API tests
- print PASS/FAIL summary
- return exit code `0` on success, `1` on failure

### Manual test execution (optional)
From `repo/pure_backend/`:

```bash
mvn test
```

---

## 5) Main API Groups

- Auth: `/api/v1/auth/*`
- Property: `/api/v1/properties/*`
- Appointment: `/api/v1/appointments/*`
- Finance: `/api/v1/finance/*`
- Audit: audit log is persisted for key mutation operations

For full contracts, see:
- `../docs/api-spec.md`

For architecture details:
- `../docs/design.md`

For ambiguity decisions:
- `../docs/questions.md`
