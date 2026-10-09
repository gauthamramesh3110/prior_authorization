ALTER TABLE policy
    ADD COLUMN source_file_name TEXT,
    ADD COLUMN ingestion_status TEXT NOT NULL DEFAULT 'NOT_INGESTED';