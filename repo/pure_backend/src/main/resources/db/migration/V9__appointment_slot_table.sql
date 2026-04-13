CREATE TABLE IF NOT EXISTS appointment_slot (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    staff_id BIGINT UNSIGNED NOT NULL,
    resource_id BIGINT UNSIGNED NOT NULL,
    start_time DATETIME(3) NOT NULL,
    end_time DATETIME(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_slot_staff_time (staff_id, start_time, end_time, status),
    KEY idx_slot_resource_time (resource_id, start_time, end_time, status),
    CONSTRAINT chk_slot_time_range CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
