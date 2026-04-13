CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    secondary_password_hash VARCHAR(255) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_account_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_account_role (
    user_id BIGINT UNSIGNED NOT NULL,
    role VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_account_role_user FOREIGN KEY (user_id) REFERENCES user_account(id)
        ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS financial_refund (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    refund_no VARCHAR(64) NOT NULL,
    transaction_no VARCHAR(64) NOT NULL,
    refund_mode VARCHAR(32) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    reason VARCHAR(256) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no),
    KEY idx_refund_tx_no (transaction_no),
    CONSTRAINT chk_refund_amount_positive CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS daily_statement (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    statement_date DATE NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL,
    transaction_count INT NOT NULL,
    has_exception TINYINT(1) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_statement_date (statement_date),
    KEY idx_statement_status_date (status, statement_date),
    CONSTRAINT chk_statement_total_non_negative CHECK (total_amount >= 0),
    CONSTRAINT chk_statement_count_non_negative CHECK (transaction_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoice_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    statement_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(128) NOT NULL,
    tax_no VARCHAR(64) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    invoice_no VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL,
    issued_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_invoice_statement_status (statement_id, status),
    KEY idx_invoice_no (invoice_no),
    CONSTRAINT fk_invoice_statement FOREIGN KEY (statement_id) REFERENCES daily_statement(id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_invoice_amount_positive CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
