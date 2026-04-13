CREATE TABLE IF NOT EXISTS settlement_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    settlement_no VARCHAR(64) NOT NULL,
    settlement_date DATETIME(3) NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_settlement_no (settlement_no),
    KEY idx_settlement_status_date (status, settlement_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
