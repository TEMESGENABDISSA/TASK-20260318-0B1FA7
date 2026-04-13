ALTER TABLE file_upload_session
    ADD COLUMN mime_type VARCHAR(128) NULL,
    ADD COLUMN file_size BIGINT UNSIGNED NULL,
    ADD COLUMN chunk_size INT UNSIGNED NULL;

