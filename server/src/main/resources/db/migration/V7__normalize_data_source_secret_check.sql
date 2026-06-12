ALTER TABLE platform.data_source
    DROP CONSTRAINT IF EXISTS ck_data_source_config_no_plain_secret;

ALTER TABLE platform.data_source
    ADD CONSTRAINT ck_data_source_config_no_plain_secret CHECK (
        NOT jsonb_exists_any(
            config_json,
            ARRAY[
                'password',
                'token',
                'secret',
                'connectionString',
                'connection_string',
                'privateKey',
                'private_key'
            ]::text[]
        )
    );
