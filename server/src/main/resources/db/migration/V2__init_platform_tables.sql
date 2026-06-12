CREATE SCHEMA IF NOT EXISTS platform;

CREATE TABLE platform.data_source (
    id uuid PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    code varchar(100) NOT NULL,
    name varchar(200) NOT NULL,
    type varchar(50) NOT NULL,
    environment varchar(50) NOT NULL DEFAULT 'default',
    status varchar(30) NOT NULL DEFAULT 'DISABLED',
    config_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    secret_ref varchar(255),
    created_by varchar(100),
    updated_by varchar(100),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_data_source_code UNIQUE (code),
    CONSTRAINT ck_data_source_config_no_plain_secret CHECK (
        NOT (config_json ?| ARRAY[
            'password', 'token', 'secret', 'connectionString', 'connection_string', 'privateKey', 'private_key'
        ])
    )
);

COMMENT ON COLUMN platform.data_source.config_json IS '仅保存非敏感连接配置，不得保存明文密码、Token、连接串或密钥';
COMMENT ON COLUMN platform.data_source.secret_ref IS '服务端密钥引用，不保存密钥明文';

CREATE TABLE platform.data_source_health_log (
    id uuid PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    data_source_id uuid NOT NULL,
    result varchar(30) NOT NULL,
    latency_ms bigint,
    error_code varchar(100),
    error_summary varchar(500),
    trace_id varchar(100),
    created_by varchar(100),
    updated_by varchar(100),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_data_source_health_log_data_source
        FOREIGN KEY (data_source_id) REFERENCES platform.data_source (id)
);

CREATE TABLE platform.component_template (
    id uuid PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    template_code varchar(100) NOT NULL,
    name varchar(200) NOT NULL,
    category varchar(100) NOT NULL,
    render_engine varchar(50) NOT NULL,
    current_version varchar(50),
    status varchar(30) NOT NULL DEFAULT 'DISABLED',
    license_scope varchar(200),
    signature varchar(500),
    checksum varchar(128),
    created_by varchar(100),
    updated_by varchar(100),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_component_template_code UNIQUE (template_code)
);

CREATE TABLE platform.component_template_version (
    id uuid PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    template_id uuid NOT NULL,
    version varchar(50) NOT NULL,
    schema_version varchar(50) NOT NULL DEFAULT '1.0',
    data_contract_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    default_config_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    field_mapping_schema_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    checksum varchar(128),
    created_by varchar(100),
    updated_by varchar(100),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_component_template_version_template
        FOREIGN KEY (template_id) REFERENCES platform.component_template (id),
    CONSTRAINT uk_component_template_version UNIQUE (template_id, version)
);

CREATE TABLE platform.dashboard (
    id uuid PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    dashboard_code varchar(100) NOT NULL,
    name varchar(200) NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'DRAFT',
    current_published_version_id uuid,
    created_by varchar(100),
    updated_by varchar(100),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_dashboard_code UNIQUE (dashboard_code)
);

CREATE TABLE platform.dashboard_draft (
    id uuid PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    dashboard_id uuid NOT NULL,
    revision bigint NOT NULL DEFAULT 1,
    schema_version varchar(50) NOT NULL DEFAULT '1.0',
    config_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_by varchar(100),
    updated_by varchar(100),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dashboard_draft_dashboard
        FOREIGN KEY (dashboard_id) REFERENCES platform.dashboard (id),
    CONSTRAINT uk_dashboard_draft_dashboard UNIQUE (dashboard_id)
);

CREATE TABLE platform.dashboard_card (
    id uuid PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    dashboard_id uuid NOT NULL,
    card_code varchar(100) NOT NULL,
    template_code varchar(100) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    ai_enabled boolean NOT NULL DEFAULT false,
    schema_version varchar(50) NOT NULL DEFAULT '1.0',
    config_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_by varchar(100),
    updated_by varchar(100),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dashboard_card_dashboard
        FOREIGN KEY (dashboard_id) REFERENCES platform.dashboard (id),
    CONSTRAINT uk_dashboard_card_code UNIQUE (dashboard_id, card_code)
);

CREATE TABLE platform.dashboard_version (
    id uuid PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    dashboard_id uuid NOT NULL,
    version_no varchar(50) NOT NULL,
    schema_version varchar(50) NOT NULL DEFAULT '1.0',
    snapshot_json jsonb NOT NULL,
    checksum varchar(128) NOT NULL,
    published_by varchar(100),
    published_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by varchar(100),
    updated_by varchar(100),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dashboard_version_dashboard
        FOREIGN KEY (dashboard_id) REFERENCES platform.dashboard (id),
    CONSTRAINT uk_dashboard_version_no UNIQUE (dashboard_id, version_no)
);

COMMENT ON TABLE platform.dashboard_version IS '大屏发布后的不可变版本快照，只允许新增，不允许更新或删除';
COMMENT ON COLUMN platform.dashboard_version.snapshot_json IS '发布时完整序列化的大屏不可变快照';

ALTER TABLE platform.dashboard
    ADD CONSTRAINT fk_dashboard_current_published_version
    FOREIGN KEY (current_published_version_id) REFERENCES platform.dashboard_version (id);

CREATE TABLE platform.dashboard_publish_log (
    id uuid PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    dashboard_id uuid NOT NULL,
    version_id uuid,
    action varchar(50) NOT NULL,
    result varchar(30) NOT NULL,
    error_code varchar(100),
    trace_id varchar(100),
    created_by varchar(100),
    updated_by varchar(100),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dashboard_publish_log_dashboard
        FOREIGN KEY (dashboard_id) REFERENCES platform.dashboard (id),
    CONSTRAINT fk_dashboard_publish_log_version
        FOREIGN KEY (version_id) REFERENCES platform.dashboard_version (id)
);

CREATE TABLE platform.card_query_log (
    id uuid PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    dashboard_id uuid,
    card_id uuid,
    data_source_id uuid,
    sql_hash varchar(128),
    duration_ms bigint,
    result varchar(30) NOT NULL,
    error_code varchar(100),
    trace_id varchar(100),
    created_by varchar(100),
    updated_by varchar(100),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_query_log_dashboard
        FOREIGN KEY (dashboard_id) REFERENCES platform.dashboard (id),
    CONSTRAINT fk_card_query_log_card
        FOREIGN KEY (card_id) REFERENCES platform.dashboard_card (id),
    CONSTRAINT fk_card_query_log_data_source
        FOREIGN KEY (data_source_id) REFERENCES platform.data_source (id)
);

CREATE TABLE platform.runtime_error_log (
    id uuid PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    resource_type varchar(100) NOT NULL,
    resource_id varchar(100),
    error_code varchar(100) NOT NULL,
    error_summary varchar(500) NOT NULL,
    diagnostic_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    trace_id varchar(100),
    created_by varchar(100),
    updated_by varchar(100),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE platform.audit_log (
    id uuid PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    operator_id varchar(100),
    action varchar(100) NOT NULL,
    resource_type varchar(100) NOT NULL,
    resource_id varchar(100),
    change_summary varchar(1000),
    result varchar(30) NOT NULL DEFAULT 'SUCCESS',
    trace_id varchar(100),
    created_by varchar(100),
    updated_by varchar(100),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_data_source_status ON platform.data_source (status);
CREATE INDEX idx_data_source_environment ON platform.data_source (environment);
CREATE INDEX idx_data_source_health_log_source ON platform.data_source_health_log (data_source_id);
CREATE INDEX idx_data_source_health_log_trace ON platform.data_source_health_log (trace_id);
CREATE INDEX idx_component_template_status ON platform.component_template (status);
CREATE INDEX idx_component_template_category ON platform.component_template (category);
CREATE INDEX idx_component_template_version_template ON platform.component_template_version (template_id);
CREATE INDEX idx_dashboard_status ON platform.dashboard (status);
CREATE INDEX idx_dashboard_draft_dashboard ON platform.dashboard_draft (dashboard_id);
CREATE INDEX idx_dashboard_card_dashboard ON platform.dashboard_card (dashboard_id);
CREATE INDEX idx_dashboard_card_template_code ON platform.dashboard_card (template_code);
CREATE INDEX idx_dashboard_version_dashboard ON platform.dashboard_version (dashboard_id);
CREATE INDEX idx_dashboard_publish_log_dashboard ON platform.dashboard_publish_log (dashboard_id);
CREATE INDEX idx_dashboard_publish_log_trace ON platform.dashboard_publish_log (trace_id);
CREATE INDEX idx_card_query_log_dashboard ON platform.card_query_log (dashboard_id);
CREATE INDEX idx_card_query_log_trace ON platform.card_query_log (trace_id);
CREATE INDEX idx_runtime_error_log_trace ON platform.runtime_error_log (trace_id);
CREATE INDEX idx_runtime_error_log_resource ON platform.runtime_error_log (resource_type, resource_id);
CREATE INDEX idx_audit_log_trace ON platform.audit_log (trace_id);
CREATE INDEX idx_audit_log_resource ON platform.audit_log (resource_type, resource_id);

CREATE OR REPLACE FUNCTION platform.prevent_dashboard_version_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'dashboard_version is immutable; create a new version instead';
END;
$$;

CREATE TRIGGER trg_dashboard_version_immutable
BEFORE UPDATE OR DELETE ON platform.dashboard_version
FOR EACH ROW
EXECUTE FUNCTION platform.prevent_dashboard_version_mutation();


