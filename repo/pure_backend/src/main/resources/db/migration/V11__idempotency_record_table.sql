CREATE TABLE IF NOT EXISTS idempotency_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    scope VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    fingerprint VARCHAR(128) NOT NULL,
    expire_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency_scope_key (scope, idempotency_key),
    KEY idx_idempotency_expire_at (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
