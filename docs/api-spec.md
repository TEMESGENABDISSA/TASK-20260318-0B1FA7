# Anju Accompanying Medical Appointment Operation Management System  
## API Specification (Pure Backend)

## 1. API Conventions

### 1.1 Base Information
- Base URL: `/api/v1`
- Content-Type: `application/json`
- Authentication: Bearer token (except login/refresh)
- Time format: ISO-8601 (`yyyy-MM-dd'T'HH:mm:ssXXX`)
- Currency: RMB, decimal(18,2)

### 1.2 Standard Response Envelope
Success:
```json
{
  "code": 0,
  "msg": "OK",
  "data": {}
}
```

Error:
```json
{
  "code": 400,
  "msg": "Validation failed"
}
```

### 1.3 Common Error Codes
- `400` Validation error / business rule rejection
- `401` Unauthorized (invalid/expired token)
- `403` Forbidden (insufficient permission)
- `404` Resource not found
- `409` Resource conflict / idempotency conflict
- `500` Internal server error

Note: endpoint sections may include legacy logical labels (e.g., `400100`, `409010`) to describe business semantics.
At runtime, the top-level `code` field follows the implemented HTTP-style numeric codes above.

### 1.4 Naming and REST Consistency Rules
- Resource names are plural nouns: `/properties`, `/appointments`, `/files`.
- Query/list operations use `GET /resources`.
- Create uses `POST /resources`, update uses `PUT /resources/{id}`, delete/soft-delete uses `DELETE /resources/{id}`.
- Business commands use action suffix style: `POST /resources/{id}:action`.
- Every error response MUST follow the standard envelope from section 1.2.

---

## 2. Authentication APIs

### 2.1 Login
- **Method**: `POST`
- **URL**: `/api/v1/auth/login`
- **Request Body**:
```json
{
  "username": "dispatcher01",
  "password": "Passw0rd123"
}
```
- **Response**:
```json
{
  "code": 0,
  "msg": "OK",
  "data": {
    "accessToken": "jwt-token",
    "refreshToken": "refresh-token",
    "expiresIn": 7200,
    "user": {
      "id": 101,
      "username": "dispatcher01",
      "roles": ["DISPATCHER"]
    }
  }
}
```
- **Error Responses**:
  - `401001` invalid credentials
  - `400100` password policy not met

### 2.2 Refresh Token
- **Method**: `POST`
- **URL**: `/api/v1/auth/refresh`
- **Request Body**:
```json
{
  "refreshToken": "refresh-token"
}
```
- **Response**: new access token payload
- **Error Responses**:
  - `401001` refresh token invalid/expired

### 2.3 Secondary Password Verify (Sensitive Operations)
- **Method**: `POST`
- **URL**: `/api/v1/auth/secondary-verify`
- **Request Body**:
```json
{
  "operation": "FINANCE_REFUND_APPROVE",
  "secondaryPassword": "SecP@ss123"
}
```
- **Response**:
```json
{
  "code": 0,
  "msg": "OK",
  "data": {
    "verified": true,
    "verificationToken": "sec-verify-token",
    "validSeconds": 300
  }
}
```
- **Error Responses**:
  - `403002` secondary verification failed

### 2.4 Get Current User Profile
- **Method**: `GET`
- **URL**: `/api/v1/auth/me`
- **Request Body**: none
- **Response**: current user id, username, roles, permissions summary
- **Error Responses**:
  - `401001` unauthorized

### 2.5 Logout
- **Method**: `POST`
- **URL**: `/api/v1/auth/logout`
- **Request Body**:
```json
{
  "refreshToken": "refresh-token"
}
```
- **Response**: logout success
- **Error Responses**:
  - `401001` invalid token

---

## 3. Property Management APIs

### 3.1 Create Property
- **Method**: `POST`
- **URL**: `/api/v1/properties`
- **Request Body**:
```json
{
  "propertyCode": "P20260413001",
  "name": "Anju Service Apartment A",
  "rent": 3800.00,
  "deposit": 3800.00,
  "rentalStartDate": "2026-04-15",
  "rentalEndDate": "2026-12-31",
  "vacancyPeriods": [
    {"startDate": "2026-05-01", "endDate": "2026-05-10"}
  ],
  "complianceFields": {
    "licenseNo": "LIC-2026-001",
    "ownerContactMasked": "138****8899"
  }
}
```
- **Response**:
```json
{
  "code": 0,
  "msg": "OK",
  "data": {
    "id": 10001,
    "propertyCode": "P20260413001",
    "status": "DRAFT"
  }
}
```
- **Error Responses**:
  - `400100` required field/date format invalid
  - `409001` duplicate property code

### 3.2 Update Property
- **Method**: `PUT`
- **URL**: `/api/v1/properties/{propertyId}`
- **Request Body**: partial/full property payload
- **Response**: updated property summary
- **Error Responses**:
  - `404001` property not found
  - `422001` invalid update in current status

### 3.3 List Properties
- **Method**: `GET`
- **URL**: `/api/v1/properties?status=APPROVED&page=1&size=20&startDate=2026-04-01&endDate=2026-04-30`
- **Request Body**: none
- **Response**:
```json
{
  "code": 0,
  "msg": "OK",
  "data": {
    "items": [
      {"id": 10001, "propertyCode": "P20260413001", "status": "APPROVED"}
    ],
    "page": 1,
    "size": 20,
    "total": 1
  }
}
```
- **Error Responses**:
  - `400100` invalid pagination/filter values

### 3.4 Get Property Detail
- **Method**: `GET`
- **URL**: `/api/v1/properties/{propertyId}`
- **Request Body**: none
- **Response**: full property detail including rent/deposit, rental windows, compliance result
- **Error Responses**:
  - `404001` property not found

### 3.5 Attach Materials
- **Method**: `POST`
- **URL**: `/api/v1/properties/{propertyId}/materials`
- **Request Body**:
```json
{
  "materials": [
    {"fileId": 9001, "type": "IMAGE"},
    {"fileId": 9002, "type": "VIDEO"}
  ]
}
```
- **Response**: material linkage result
- **Error Responses**:
  - `404001` property/file not found

### 3.6 Submit for Review
- **Method**: `POST`
- **URL**: `/api/v1/properties/{propertyId}:submit-review`
- **Request Body**:
```json
{
  "comment": "All compliance fields completed."
}
```
- **Response**: status -> `PENDING_REVIEW`
- **Error Responses**:
  - `422001` invalid state transition

### 3.7 Approve or Reject Property
- **Method**: `POST`
- **URL**: `/api/v1/properties/{propertyId}:review`
- **Request Body**:
```json
{
  "action": "APPROVE",
  "reviewComment": "Validated."
}
```
- **Response**: status updated (`APPROVED`/`REJECTED`)
- **Error Responses**:
  - `403001` reviewer role required
  - `422001` invalid state transition

### 3.8 Invalidate Property
- **Method**: `POST`
- **URL**: `/api/v1/properties/{propertyId}:invalidate`
- **Request Body**:
```json
{
  "reason": "Contract terminated."
}
```
- **Response**: status -> `INVALIDATED`
- **Error Responses**:
  - `403002` secondary password verification required

---

## 4. Appointment Scheduling APIs

### 4.1 Create Appointment
- **Method**: `POST`
- **URL**: `/api/v1/appointments`
- **Headers**: `Idempotency-Key: <uuid>`
- **Request Body**:
```json
{
  "propertyId": 10001,
  "serviceType": "STANDARD_VISIT",
  "durationMinutes": 60,
  "staffId": 3001,
  "resourceId": 5001,
  "startTime": "2026-04-20T09:00:00+08:00",
  "customer": {
    "name": "Li Hua",
    "phone": "13900001111",
    "idNoMasked": "1101********1234"
  }
}
```
- **Response**:
```json
{
  "code": 0,
  "msg": "OK",
  "data": {
    "appointmentNo": "AP202604130001",
    "status": "PENDING_CONFIRMATION",
    "rescheduleCount": 0
  }
}
```
- **Error Responses**:
  - `400100` invalid duration (must be 15/30/60/90)
  - `409001` slot conflict (staff/resource overlap)
  - `409010` duplicate idempotency key

### 4.2 Create Available Time Slot
- **Method**: `POST`
- **URL**: `/api/v1/appointment-slots`
- **Request Body**:
```json
{
  "staffId": 3001,
  "resourceId": 5001,
  "startTime": "2026-04-20T09:00:00+08:00",
  "endTime": "2026-04-20T12:00:00+08:00"
}
```
- **Response**: slot created with `slotId`
- **Error Responses**:
  - `409001` slot overlap
  - `400100` invalid time range

### 4.3 List Available Time Slots
- **Method**: `GET`
- **URL**: `/api/v1/appointment-slots?staffId=3001&from=2026-04-20T00:00:00+08:00&to=2026-04-20T23:59:59+08:00`
- **Request Body**: none
- **Response**: available slot list
- **Error Responses**:
  - `400100` invalid time window

### 4.4 Get Appointment Detail
- **Method**: `GET`
- **URL**: `/api/v1/appointments/{appointmentNo}`
- **Request Body**: none
- **Response**: full appointment detail with status, penalty, timeline
- **Error Responses**:
  - `404001` appointment not found

### 4.5 List Appointments
- **Method**: `GET`
- **URL**: `/api/v1/appointments?status=CONFIRMED&from=2026-04-01T00:00:00+08:00&to=2026-04-30T23:59:59+08:00&page=1&size=20`
- **Request Body**: none
- **Response**: paginated list
- **Error Responses**:
  - `400100` invalid filters

### 4.6 Confirm Appointment
- **Method**: `POST`
- **URL**: `/api/v1/appointments/{appointmentNo}:confirm`
- **Request Body**:
```json
{
  "confirmBy": "STAFF",
  "note": "Confirmed by staff."
}
```
- **Response**: status -> `CONFIRMED`
- **Error Responses**:
  - `422001` invalid state transition

### 4.7 Start Service
- **Method**: `POST`
- **URL**: `/api/v1/appointments/{appointmentNo}:start-service`
- **Request Body**:
```json
{
  "note": "Service started on site"
}
```
- **Response**: status -> `IN_SERVICE`
- **Error Responses**:
  - `422001` invalid state transition

### 4.8 Complete Service
- **Method**: `POST`
- **URL**: `/api/v1/appointments/{appointmentNo}:complete`
- **Request Body**:
```json
{
  "serviceSummary": "Completed successfully"
}
```
- **Response**: status -> `COMPLETED`
- **Error Responses**:
  - `422001` invalid state transition

### 4.9 Reschedule Appointment
- **Method**: `POST`
- **URL**: `/api/v1/appointments/{appointmentNo}:reschedule`
- **Headers**: `Idempotency-Key: <uuid>`
- **Request Body**:
```json
{
  "newStartTime": "2026-04-21T10:00:00+08:00",
  "durationMinutes": 60,
  "reason": "Customer request"
}
```
- **Response**: new slot + `rescheduleCount`
- **Error Responses**:
  - `400100` less than 24h before start
  - `409001` target slot conflict
  - `422001` reschedule limit exceeded (>2) or invalid status

### 4.10 Cancel Appointment
- **Method**: `POST`
- **URL**: `/api/v1/appointments/{appointmentNo}:cancel`
- **Request Body**:
```json
{
  "reason": "Customer no longer needed service"
}
```
- **Response**:
```json
{
  "code": 0,
  "msg": "OK",
  "data": {
    "status": "CANCELLED",
    "penaltyAmount": 50.00
  }
}
```
- **Error Responses**:
  - `422001` invalid state transition
  - `400100` cancellation policy violation

### 4.11 Release Expired Pending Appointments (Internal Scheduler API)
- **Method**: `POST`
- **URL**: `/api/v1/internal/appointments:release-expired`
- **Request Body**:
```json
{
  "beforeTime": "2026-04-13T10:15:00+08:00"
}
```
- **Response**: released count
- **Error Responses**:
  - `403001` internal service role required

---

## 5. Financial Transaction APIs

### 5.1 Create Transaction (Bookkeeping)
- **Method**: `POST`
- **URL**: `/api/v1/finance/transactions`
- **Headers**: `Idempotency-Key: <uuid>`
- **Request Body**:
```json
{
  "appointmentNo": "AP202604130001",
  "transactionNo": "TX202604130001",
  "channel": "BANK_TRANSFER",
  "amount": 680.00,
  "currency": "CNY",
  "refundable": true,
  "occurredAt": "2026-04-13T11:00:00+08:00"
}
```
- **Response**: transaction record id + status
- **Error Responses**:
  - `409001` duplicate transaction number
  - `409010` duplicate idempotency key

### 5.2 Get Transaction Detail
- **Method**: `GET`
- **URL**: `/api/v1/finance/transactions/{transactionNo}`
- **Request Body**: none
- **Response**: transaction detail including refundable status and linked appointment
- **Error Responses**:
  - `404001` transaction not found

### 5.3 List Transactions
- **Method**: `GET`
- **URL**: `/api/v1/finance/transactions?channel=BANK_TRANSFER&from=2026-04-01&to=2026-04-30&page=1&size=20`
- **Request Body**: none
- **Response**: paginated transaction list
- **Error Responses**:
  - `400100` invalid filter values

### 5.4 Create Refund
- **Method**: `POST`
- **URL**: `/api/v1/finance/refunds`
- **Headers**: `Idempotency-Key: <uuid>`
- **Request Body**:
```json
{
  "transactionNo": "TX202604130001",
  "refundAmount": 200.00,
  "refundMode": "ORIGINAL_ROUTE",
  "reason": "Service cancellation"
}
```
- **Response**: refund number + refund status
- **Error Responses**:
  - `403002` secondary password verification required
  - `422001` non-refundable transaction or amount over limit

### 5.5 Create Settlement Record
- **Method**: `POST`
- **URL**: `/api/v1/finance/settlements`
- **Request Body**:
```json
{
  "settlementDate": "2026-04-13",
  "transactionNos": ["TX202604130001"],
  "operatorNote": "Daily settlement batch #1"
}
```
- **Response**: settlement number, total amount
- **Error Responses**:
  - `400100` invalid settlement input

### 5.6 Generate Daily Statement
- **Method**: `POST`
- **URL**: `/api/v1/finance/statements:generate`
- **Request Body**:
```json
{
  "date": "2026-04-13"
}
```
- **Response**: statement id, generation status
- **Error Responses**:
  - `409001` statement already exists

### 5.7 List Statements
- **Method**: `GET`
- **URL**: `/api/v1/finance/statements?dateFrom=2026-04-01&dateTo=2026-04-30&hasException=true`
- **Request Body**: none
- **Response**: paginated statement summaries
- **Error Responses**:
  - `400100` invalid date range

### 5.8 Export Statement
- **Method**: `POST`
- **URL**: `/api/v1/finance/statements/{statementId}:export`
- **Request Body**:
```json
{
  "format": "CSV"
}
```
- **Response**: export task id / download reference
- **Error Responses**:
  - `404001` statement not found

### 5.9 Create Invoice Request
- **Method**: `POST`
- **URL**: `/api/v1/finance/invoices/requests`
- **Request Body**:
```json
{
  "statementId": 88001,
  "title": "Anju Operations Ltd.",
  "taxNo": "9131XXXXXXXXXXXX",
  "amount": 1280.00
}
```
- **Response**: invoice request id + status `REQUESTED`
- **Error Responses**:
  - `400100` invalid invoice fields
  - `404001` statement not found

### 5.10 Mark Invoice Issued
- **Method**: `POST`
- **URL**: `/api/v1/finance/invoices/{invoiceRequestId}:mark-issued`
- **Request Body**:
```json
{
  "invoiceNo": "INV202604130001",
  "issuedAt": "2026-04-13T16:00:00+08:00"
}
```
- **Response**: status -> `ISSUED`
- **Error Responses**:
  - `403001` finance role required
  - `422001` invalid state transition

---

## 6. File Management APIs

### 6.1 Init Chunk Upload
- **Method**: `POST`
- **URL**: `/api/v1/files/uploads:init`
- **Request Body**:
```json
{
  "fileName": "medical-report.pdf",
  "contentHash": "sha256-hex-value",
  "fileSize": 10485760,
  "chunkSize": 1048576,
  "mimeType": "application/pdf"
}
```
- **Response**:
```json
{
  "code": 0,
  "msg": "OK",
  "data": {
    "uploadId": "UP-20260413-0001",
    "instantUpload": false,
    "uploadedChunks": [0,1]
  }
}
```
- **Error Responses**:
  - `400100` invalid chunk/file params
  - `409001` upload session conflict

### 6.2 Upload Chunk
- **Method**: `POST`
- **URL**: `/api/v1/files/uploads/{uploadId}/chunks/{chunkIndex}`
- **Request Params**:
  - `chunkBytes` (optional, default `524288`)
  - `file` (required, multipart file part)
- **Response**: chunk accepted status
- **Error Responses**:
  - `404001` upload session not found
  - `400100` invalid chunk index / missing chunk file

### 6.3 Complete Upload
- **Method**: `POST`
- **URL**: `/api/v1/files/uploads/{uploadId}:complete`
- **Request Body**:
```json
{
  "totalChunks": 10
}
```
- **Response**: `fileId`, version, preview metadata
- **Error Responses**:
  - `422001` missing chunks

### 6.4 Get File Metadata
- **Method**: `GET`
- **URL**: `/api/v1/files/{fileId}`
- **Request Body**: none
- **Response**: metadata, version, deletion status
- **Error Responses**:
  - `404001` file not found

### 6.5 Get File Preview URL
- **Method**: `GET`
- **URL**: `/api/v1/files/{fileId}/preview`
- **Request Body**: none
- **Response**: preview url/token and mime metadata
- **Error Responses**:
  - `404001` file not found
  - `403001` permission denied

### 6.5.1 Download/Stream File Content (Current Version)
- **Method**: `GET`
- **URL**: `/api/v1/files/{fileId}/content`
- **Request Body**: none
- **Response**: streamed binary content with `Content-Type` derived from stored `mimeType`
- **Error Responses**:
  - `404001` file not found
  - `400100` stored file content not found

### 6.6 Create New File Version
- **Method**: `POST`
- **URL**: `/api/v1/files/{fileId}:new-version`
- **Request Body**:
```json
{
  "uploadId": "UP-20260413-0002",
  "changeNote": "Updated with corrected data."
}
```
- **Response**: new version number
- **Error Responses**:
  - `404001` file not found

### 6.7 Rollback File Version
- **Method**: `POST`
- **URL**: `/api/v1/files/{fileId}:rollback`
- **Request Body**:
```json
{
  "targetVersion": 2,
  "reason": "Version 3 content invalid"
}
```
- **Response**: active version updated
- **Error Responses**:
  - `422001` target version invalid

### 6.8 Move to Recycle Bin
- **Method**: `DELETE`
- **URL**: `/api/v1/files/{fileId}`
- **Request Body**: none
- **Response**: deletion flag true + expireAt (+30 days)
- **Error Responses**:
  - `404001` file not found

### 6.9 Restore from Recycle Bin
- **Method**: `POST`
- **URL**: `/api/v1/files/{fileId}:restore`
- **Request Body**: none
- **Response**: restored metadata
- **Error Responses**:
  - `422001` recycle bin retention expired

### 6.10 List Recycle Bin Files
- **Method**: `GET`
- **URL**: `/api/v1/files/recycle-bin?page=1&size=20`
- **Request Body**: none
- **Response**: paginated deleted file list with `expireAt`
- **Error Responses**:
  - `400100` invalid pagination

### 6.11 Permanently Delete File
- **Method**: `DELETE`
- **URL**: `/api/v1/files/{fileId}:purge`
- **Request Body**: none
- **Response**: purge success
- **Error Responses**:
  - `403002` secondary password verification required
  - `404001` file not found

---

## 7. Audit Log APIs

### 7.1 Query Audit Logs
- **Method**: `GET`
- **URL**: `/api/v1/audit/logs?page=1&size=50`
- **Request Body**: none
- **Response**:
```json
{
  "code": 0,
  "msg": "OK",
  "data": {
    "items": [
      {
        "id": 70001,
        "operator": "admin",
        "action": "APPOINTMENT_RESCHEDULE",
        "entityType": "Appointment",
        "entityId": "AP-1001",
        "occurredAt": "2026-04-13T14:03:00"
      }
    ],
    "page": 1,
    "size": 50,
    "total": 1
  }
}
```
- **Error Responses**:
  - `403001` audit permission required
  - `400100` invalid query parameters

### 7.2 Get Audit Log Detail
- **Method**: `GET`
- **URL**: `/api/v1/audit/logs/{logId}`
- **Request Body**: none
- **Response**: full action detail including key before/after snapshots
- **Error Responses**:
  - `404001` audit log not found

### 7.3 Export Audit Logs
- **Method**: `POST`
- **URL**: `/api/v1/audit/logs:export`
- **Request Body**:
```json
{
  "module": "FINANCE",
  "from": "2026-04-01T00:00:00+08:00",
  "to": "2026-04-30T23:59:59+08:00",
  "format": "CSV"
}
```
- **Response**: export task id (placeholder; implementation returns task-style reference)
- **Error Responses**:
  - `403001` export permission required

---

## 8. Security, Idempotency, and Compliance Rules (API Level)

- All write operations affecting appointments and finance should support `Idempotency-Key`.
- Sensitive APIs (refund approve, property invalidation, permission changes) require successful secondary verification token.
- Sensitive data fields in API responses must be masked by default (e.g., phone, ID number).
- Validation errors always return structured JSON using unified error format.
- No stack trace or internal SQL error is exposed to API consumers.

---

## 9. Versioning and Compatibility

- Version prefix is URI-based: `/api/v1`.
- Backward-incompatible changes require `/api/v2`.
- Additive fields are allowed in minor releases when default-safe for existing clients.

---

## 10. Implementation-Aligned Delta (Final)

This section is the source of truth for endpoints that were finalized in the current implementation pass.

### 10.1 Appointment and Slot APIs
- `POST /api/v1/appointments/{appointmentNo}:start-service`
- `POST /api/v1/appointments/{appointmentNo}:complete`
- `POST /api/v1/appointment-slots`
- `GET /api/v1/appointment-slots`
- `POST /api/v1/appointment-slots/{slotId}:invalidate`

### 10.2 File APIs
- `POST /api/v1/files/uploads/{uploadId}/chunks/{chunkIndex}?chunkBytes=<bytes>` (multipart `file`)
  - Adds request-rate + estimated bandwidth throttling and stores chunk bytes.
- `GET /api/v1/files/{fileId}/preview`
  - Returns `previewType` and `previewUrl`.
- `GET /api/v1/files/{fileId}/preview-content`
  - Returns preview capability and render mode (`STREAM`, `DOCUMENT_VIEW`, `DOWNLOAD_ONLY`).
- `GET /api/v1/files/{fileId}/content`
  - Streams current version content.

### 10.3 Import/Export Mapping APIs
- `GET /api/v1/finance/imports/field-mappings`
- `POST /api/v1/finance/imports/{domain}:validate-mapping`

Supported `domain` values:
- `transaction`
- `property`
- `appointment`

### 10.4 Sensitive Field Output Policy
- Appointment responses include masked fields:
  - `contactPhoneMasked`
  - `idNoMasked`
- Invoice responses include masked field:
  - `taxNoMasked`
- Raw sensitive values are not serialized in API output.
