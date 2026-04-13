CREATE TABLE IF NOT EXISTS property_vacancy_period (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    property_id BIGINT UNSIGNED NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    PRIMARY KEY (id),
    KEY idx_property_vacancy_property (property_id),
    CONSTRAINT fk_property_vacancy_property FOREIGN KEY (property_id) REFERENCES property(id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT chk_property_vacancy_range CHECK (end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
