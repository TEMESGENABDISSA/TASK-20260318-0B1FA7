-- Core domain schema for high-load backend:
-- 1) property
-- 2) appointment
-- 3) financial_transaction
-- 4) file_metadata
--
-- Target: MySQL 8.0+ (InnoDB, utf8mb4)
-- Notes:
-- - Composite indexes are ordered for common WHERE patterns:
--   status + time range (+ id for stable pagination).
-- - Foreign keys default to RESTRICT to prevent accidental cascading loss.
-- - CHECK constraints protect business invariants at DB level.

CREATE TABLE IF NOT EXISTS property (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
    property_code VARCHAR(64) NOT NULL COMMENT 'Business unique code',
    property_name VARCHAR(128) NOT NULL COMMENT 'Display name',
    status VARCHAR(32) NOT NULL COMMENT 'DRAFT/PENDING_REVIEW/APPROVED/REJECTED/INVALIDATED',
    rent DECIMAL(18,2) NOT NULL COMMENT 'Monthly rent',
    deposit DECIMAL(18,2) NOT NULL COMMENT 'Deposit amount',
    rental_start_date DATE NOT NULL COMMENT 'Available rental start date',
    rental_end_date DATE NOT NULL COMMENT 'Available rental end date',
    compliance_result VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'UNKNOWN/PASS/FAIL',
    created_by BIGINT UNSIGNED NULL COMMENT 'Operator user id',
    updated_by BIGINT UNSIGNED NULL COMMENT 'Operator user id',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    PRIMARY KEY (id),
    UNIQUE KEY uk_property_code (property_code),
    KEY idx_property_status_created_id (status, created_at, id),
    KEY idx_property_status_rental_range_id (status, rental_start_date, rental_end_date, id),
    KEY idx_property_compliance_status_updated (compliance_result, status, updated_at),
    CONSTRAINT chk_property_rent_non_negative CHECK (rent >= 0),
    CONSTRAINT chk_property_deposit_non_negative CHECK (deposit >= 0),
    CONSTRAINT chk_property_rental_range CHECK (rental_end_date >= rental_start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Property master table';


CREATE TABLE IF NOT EXISTS appointment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
    appointment_no VARCHAR(64) NOT NULL COMMENT 'Business unique appointment number',
    property_id BIGINT UNSIGNED NOT NULL COMMENT 'FK -> property.id',
    status VARCHAR(32) NOT NULL COMMENT 'CREATED/PENDING_CONFIRMATION/CONFIRMED/IN_SERVICE/COMPLETED/CANCELLED/EXPIRED_RELEASED',
    service_type VARCHAR(64) NOT NULL COMMENT 'Service type enum value',
    duration_minutes SMALLINT UNSIGNED NOT NULL COMMENT 'Allowed: 15/30/60/90',
    staff_id BIGINT UNSIGNED NOT NULL COMMENT 'Assigned staff id',
    resource_id BIGINT UNSIGNED NOT NULL COMMENT 'Assigned resource id',
    start_time DATETIME(3) NOT NULL COMMENT 'Appointment start',
    end_time DATETIME(3) NOT NULL COMMENT 'Appointment end',
    order_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Order amount for penalty computation',
    penalty_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Cancellation penalty',
    reschedule_count TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Max 2 by business rule',
    confirm_deadline DATETIME(3) NOT NULL COMMENT 'Auto-release threshold',
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    PRIMARY KEY (id),
    UNIQUE KEY uk_appointment_no (appointment_no),
    KEY idx_appt_status_start_end_id (status, start_time, end_time, id),
    KEY idx_appt_staff_status_start_end (staff_id, status, start_time, end_time),
    KEY idx_appt_resource_status_start_end (resource_id, status, start_time, end_time),
    KEY idx_appt_confirm_deadline_status (confirm_deadline, status),
    KEY idx_appt_property_status_start (property_id, status, start_time),
    KEY idx_appt_property_id (property_id),
    CONSTRAINT fk_appt_property FOREIGN KEY (property_id) REFERENCES property (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_appt_time_range CHECK (end_time > start_time),
    CONSTRAINT chk_appt_duration CHECK (duration_minutes IN (15, 30, 60, 90)),
    CONSTRAINT chk_appt_order_amount_non_negative CHECK (order_amount >= 0),
    CONSTRAINT chk_appt_penalty_non_negative CHECK (penalty_amount >= 0),
    CONSTRAINT chk_appt_reschedule_count_max CHECK (reschedule_count <= 2),
    CONSTRAINT chk_appt_confirm_deadline_before_start CHECK (confirm_deadline <= start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Appointment table';


CREATE TABLE IF NOT EXISTS financial_transaction (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
    transaction_no VARCHAR(64) NOT NULL COMMENT 'Business unique transaction number',
    appointment_id BIGINT UNSIGNED NOT NULL COMMENT 'FK -> appointment.id',
    status VARCHAR(32) NOT NULL COMMENT 'PENDING/SUCCESS/FAILED/REFUNDED/PARTIAL_REFUNDED',
    channel VARCHAR(32) NOT NULL COMMENT 'CASH/BANK_TRANSFER/ALIPAY/WECHAT/etc',
    amount DECIMAL(18,2) NOT NULL COMMENT 'Transaction amount',
    refundable TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=true,0=false',
    refund_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Accumulated refunded amount',
    settled TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Settlement flag',
    occurred_at DATETIME(3) NOT NULL COMMENT 'Business occurrence time',
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transaction_no (transaction_no),
    KEY idx_tx_status_occurred_id (status, occurred_at, id),
    KEY idx_tx_settled_status_occurred (settled, status, occurred_at),
    KEY idx_tx_channel_occurred_id (channel, occurred_at, id),
    KEY idx_tx_appointment_id (appointment_id),
    CONSTRAINT fk_tx_appointment FOREIGN KEY (appointment_id) REFERENCES appointment (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_tx_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_tx_refund_non_negative CHECK (refund_amount >= 0),
    CONSTRAINT chk_tx_refund_not_exceed_amount CHECK (refund_amount <= amount),
    CONSTRAINT chk_tx_refundable_boolean CHECK (refundable IN (0, 1)),
    CONSTRAINT chk_tx_settled_boolean CHECK (settled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Financial transaction table';


CREATE TABLE IF NOT EXISTS file_metadata (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
    file_no VARCHAR(64) NOT NULL COMMENT 'Business unique file number',
    file_name VARCHAR(255) NOT NULL COMMENT 'Original file name',
    content_hash CHAR(64) NOT NULL COMMENT 'SHA-256 hex hash for dedup',
    mime_type VARCHAR(128) NOT NULL COMMENT 'MIME type',
    file_size BIGINT UNSIGNED NOT NULL COMMENT 'Bytes',
    chunk_size INT UNSIGNED NOT NULL COMMENT 'Bytes',
    total_chunks INT UNSIGNED NOT NULL COMMENT 'Total chunk count',
    uploaded_chunks INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Uploaded chunk count',
    current_version INT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Current active version',
    status VARCHAR(32) NOT NULL COMMENT 'UPLOADING/ACTIVE/DELETED/EXPIRED',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Recycle bin flag',
    delete_expire_at DATETIME(3) NULL COMMENT 'Purge deadline after soft delete',
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_no (file_no),
    UNIQUE KEY uk_file_content_hash (content_hash),
    KEY idx_file_status_created_id (status, created_at, id),
    KEY idx_file_deleted_expire_id (is_deleted, delete_expire_at, id),
    KEY idx_file_hash_status (content_hash, status),
    CONSTRAINT chk_file_size_non_negative CHECK (file_size >= 0),
    CONSTRAINT chk_file_chunks_positive CHECK (chunk_size > 0 AND total_chunks > 0),
    CONSTRAINT chk_file_uploaded_chunks_range CHECK (uploaded_chunks <= total_chunks),
    CONSTRAINT chk_file_current_version_positive CHECK (current_version >= 1),
    CONSTRAINT chk_file_deleted_boolean CHECK (is_deleted IN (0, 1)),
    CONSTRAINT chk_file_delete_expire_logic CHECK (
        (is_deleted = 0 AND delete_expire_at IS NULL) OR
        (is_deleted = 1 AND delete_expire_at IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='File metadata and dedup table';
