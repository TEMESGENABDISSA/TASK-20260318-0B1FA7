CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    operator VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(128) NULL,
    occurred_at DATETIME(3) NOT NULL,
    before_json TEXT NULL,
    after_json TEXT NULL,
    field_changes_json TEXT NULL,
    PRIMARY KEY (id),
    KEY idx_audit_entity_time (entity_type, occurred_at),
    KEY idx_audit_operator_time (operator, occurred_at),
    KEY idx_audit_action_time (action, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
