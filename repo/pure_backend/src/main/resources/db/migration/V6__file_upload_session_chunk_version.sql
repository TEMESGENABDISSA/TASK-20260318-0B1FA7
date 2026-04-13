CREATE TABLE IF NOT EXISTS file_version (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    file_id BIGINT UNSIGNED NOT NULL,
    version_no INT UNSIGNED NOT NULL,
    change_note VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_version (file_id, version_no),
    KEY idx_file_version_file (file_id),
    CONSTRAINT fk_file_version_file FOREIGN KEY (file_id) REFERENCES file_metadata(id)
        ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS file_upload_session (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    upload_id VARCHAR(80) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_hash CHAR(64) NOT NULL,
    total_chunks INT UNSIGNED NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_upload_id (upload_id),
    KEY idx_upload_hash_status (content_hash, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS file_upload_chunk (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    upload_id VARCHAR(80) NOT NULL,
    chunk_index INT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_upload_chunk (upload_id, chunk_index),
    KEY idx_upload_chunk_upload (upload_id),
    CONSTRAINT fk_upload_chunk_session FOREIGN KEY (upload_id) REFERENCES file_upload_session(upload_id)
        ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
