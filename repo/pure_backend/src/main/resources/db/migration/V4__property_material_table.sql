CREATE TABLE IF NOT EXISTS property_material (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    property_id BIGINT UNSIGNED NOT NULL,
    file_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_property_file (property_id, file_id),
    KEY idx_property_material_property (property_id),
    CONSTRAINT fk_property_material_property FOREIGN KEY (property_id) REFERENCES property(id)
        ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
