ALTER TABLE platform.card_query_log
    ADD COLUMN IF NOT EXISTS query_type varchar(30),
    ADD COLUMN IF NOT EXISTS row_count integer,
    ADD COLUMN IF NOT EXISTS error_message varchar(500);

CREATE INDEX IF NOT EXISTS idx_card_query_log_card
    ON platform.card_query_log (card_id);

CREATE INDEX IF NOT EXISTS idx_card_query_log_data_source
    ON platform.card_query_log (data_source_id);
