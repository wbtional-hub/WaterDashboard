package com.waterdashboard.datasource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waterdashboard.common.trace.TraceIdContext;
import com.waterdashboard.datasource.dto.DataSourceListResponse;
import com.waterdashboard.datasource.dto.DataSourceRequest;
import com.waterdashboard.datasource.dto.DataSourceResponse;
import com.waterdashboard.datasource.dto.DataSourceTestRequest;
import com.waterdashboard.datasource.dto.DataSourceTestResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DataSourceService {

    private static final String SYSTEM_OPERATOR = "system";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final DataSourceSecretService secretService;
    private final DataSourceConnectionTester connectionTester;

    public DataSourceService(
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            DataSourceSecretService secretService,
            DataSourceConnectionTester connectionTester) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.secretService = secretService;
        this.connectionTester = connectionTester;
    }

    public DataSourceListResponse list(String name, String type, String status, Boolean enabled) {
        StringBuilder sql = new StringBuilder("""
                SELECT ds.id, ds.code, ds.name, ds.type, ds.environment, ds.status, ds.config_json::text AS config_json,
                       ds.created_at, ds.updated_at, last_health.last_test_time
                FROM platform.data_source ds
                LEFT JOIN (
                    SELECT data_source_id, max(created_at) AS last_test_time
                    FROM platform.data_source_health_log
                    WHERE data_source_id IS NOT NULL
                    GROUP BY data_source_id
                ) last_health ON last_health.data_source_id = ds.id
                WHERE 1 = 1
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (StringUtils.hasText(name)) {
            sql.append(" AND ds.name ILIKE :name");
            params.addValue("name", "%" + name.trim() + "%");
        }
        if (StringUtils.hasText(type)) {
            sql.append(" AND ds.type = :type");
            params.addValue("type", type.trim().toUpperCase());
        }
        if (StringUtils.hasText(status)) {
            sql.append(" AND ds.status = :status");
            params.addValue("status", status.trim().toUpperCase());
        }
        if (enabled != null) {
            sql.append(" AND ds.status = :enabledStatus");
            params.addValue("enabledStatus", enabled ? DataSourceStatus.ENABLED.name() : DataSourceStatus.DISABLED.name());
        }

        sql.append(" ORDER BY ds.updated_at DESC, ds.created_at DESC");

        List<DataSourceResponse> items = jdbcTemplate.query(sql.toString(), params, this::mapResponse);
        return new DataSourceListResponse(items, items.size());
    }

    public DataSourceResponse get(UUID id) {
        return loadInternal(id).toResponse();
    }

    @Transactional
    public DataSourceResponse create(DataSourceRequest request) {
        validateRequest(request, true);
        String secretRef = secretService.protect(request.password());
        String status = Boolean.TRUE.equals(request.enabled())
                ? DataSourceStatus.ENABLED.name()
                : DataSourceStatus.DISABLED.name();
        Map<String, Object> config = configFromRequest(request);
        UUID id = UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO platform.data_source
                    (id, code, name, type, status, config_json, secret_ref, created_by, updated_by)
                VALUES
                    (:id, :code, :name, :type, :status, CAST(:configJson AS jsonb), :secretRef, :operator, :operator)
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("code", buildCode(request.name(), id))
                .addValue("name", request.name().trim())
                .addValue("type", DataSourceType.POSTGRESQL.name())
                .addValue("status", status)
                .addValue("configJson", writeJson(config))
                .addValue("secretRef", secretRef)
                .addValue("operator", SYSTEM_OPERATOR));

        writeAudit("DATA_SOURCE_CREATE", id.toString(), "创建数据源：" + request.name().trim());
        return get(id);
    }

    @Transactional
    public DataSourceResponse update(UUID id, DataSourceRequest request) {
        validateRequest(request, false);
        InternalDataSource current = loadInternal(id);
        String secretRef = StringUtils.hasText(request.password())
                ? secretService.protect(request.password())
                : current.secretRef();
        String status = Boolean.TRUE.equals(request.enabled())
                ? DataSourceStatus.ENABLED.name()
                : DataSourceStatus.DISABLED.name();

        jdbcTemplate.update("""
                UPDATE platform.data_source
                SET name = :name,
                    type = :type,
                    status = :status,
                    config_json = CAST(:configJson AS jsonb),
                    secret_ref = :secretRef,
                    updated_by = :operator,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("name", request.name().trim())
                .addValue("type", DataSourceType.POSTGRESQL.name())
                .addValue("status", status)
                .addValue("configJson", writeJson(configFromRequest(request)))
                .addValue("secretRef", secretRef)
                .addValue("operator", SYSTEM_OPERATOR));

        writeAudit("DATA_SOURCE_UPDATE", id.toString(), "修改数据源：" + request.name().trim());
        return get(id);
    }

    @Transactional
    public DataSourceResponse disable(UUID id) {
        InternalDataSource current = loadInternal(id);
        jdbcTemplate.update("""
                UPDATE platform.data_source
                SET status = :status,
                    updated_by = :operator,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", DataSourceStatus.DISABLED.name())
                .addValue("operator", SYSTEM_OPERATOR));
        writeAudit("DATA_SOURCE_DISABLE", id.toString(), "停用数据源：" + current.name());
        return get(id);
    }

    @Transactional
    public DataSourceTestResponse testSaved(UUID id) {
        InternalDataSource dataSource = loadInternal(id);
        String password = secretService.reveal(dataSource.secretRef());
        DataSourceTestResponse result = connectionTester.test(new DataSourceTestRequest(
                dataSource.type(),
                stringConfig(dataSource.config(), "host"),
                intConfig(dataSource.config(), "port"),
                stringConfig(dataSource.config(), "database"),
                stringConfig(dataSource.config(), "username"),
                password,
                stringConfig(dataSource.config(), "schema")));
        writeHealthLog(id, result, safeTargetJson(dataSource.config()));
        return result;
    }

    @Transactional
    public DataSourceTestResponse testTemp(DataSourceTestRequest request) {
        validateTestRequest(request, true);
        DataSourceTestResponse result = connectionTester.test(new DataSourceTestRequest(
                DataSourceType.POSTGRESQL.name(),
                request.host(),
                request.port(),
                request.database(),
                request.username(),
                request.password(),
                request.schema()));
        writeHealthLog(null, result, safeTargetJson(request));
        return result;
    }

    private void validateRequest(DataSourceRequest request, boolean requirePassword) {
        if (request == null) {
            throw new IllegalArgumentException("数据源请求不能为空");
        }
        if (!StringUtils.hasText(request.name())) {
            throw new IllegalArgumentException("数据源名称不能为空");
        }
        if (!DataSourceType.POSTGRESQL.name().equalsIgnoreCase(request.type())) {
            throw new IllegalArgumentException("第一阶段仅支持 PostgreSQL 数据源");
        }
        validateTestRequest(new DataSourceTestRequest(
                request.type(),
                request.host(),
                request.port(),
                request.database(),
                request.username(),
                request.password(),
                request.schema()), requirePassword);
    }

    private void validateTestRequest(DataSourceTestRequest request, boolean requirePassword) {
        if (request == null) {
            throw new IllegalArgumentException("连接测试参数不能为空");
        }
        if (!StringUtils.hasText(request.host())) {
            throw new IllegalArgumentException("主机不能为空");
        }
        if (request.port() == null || request.port() < 1 || request.port() > 65535) {
            throw new IllegalArgumentException("端口必须在 1 到 65535 之间");
        }
        if (!StringUtils.hasText(request.database())) {
            throw new IllegalArgumentException("数据库名不能为空");
        }
        if (!StringUtils.hasText(request.username())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (requirePassword && !StringUtils.hasText(request.password())) {
            throw new IllegalArgumentException("密码不能为空");
        }
    }

    private InternalDataSource loadInternal(UUID id) {
        return jdbcTemplate.queryForObject("""
                SELECT id, code, name, type, environment, status, config_json::text AS config_json, secret_ref,
                       created_at, updated_at
                FROM platform.data_source
                WHERE id = :id
                """, new MapSqlParameterSource("id", id), (rs, rowNum) -> new InternalDataSource(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getString("environment"),
                rs.getString("status"),
                readJson(rs.getString("config_json")),
                rs.getString("secret_ref"),
                toOffsetDateTime(rs.getTimestamp("created_at")),
                toOffsetDateTime(rs.getTimestamp("updated_at")),
                null));
    }

    private DataSourceResponse mapResponse(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> config = readJson(rs.getString("config_json"));
        String status = rs.getString("status");
        return new DataSourceResponse(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getString("environment"),
                stringConfig(config, "host"),
                intConfig(config, "port"),
                stringConfig(config, "database"),
                stringConfig(config, "username"),
                stringConfig(config, "schema"),
                stringConfig(config, "remark"),
                status,
                DataSourceStatus.ENABLED.name().equals(status),
                toOffsetDateTime(rs.getTimestamp("last_test_time")),
                toOffsetDateTime(rs.getTimestamp("created_at")),
                toOffsetDateTime(rs.getTimestamp("updated_at")));
    }

    private Map<String, Object> configFromRequest(DataSourceRequest request) {
        Map<String, Object> config = new HashMap<>();
        config.put("host", request.host().trim());
        config.put("port", request.port());
        config.put("database", request.database().trim());
        config.put("username", request.username().trim());
        config.put("schema", StringUtils.hasText(request.schema()) ? request.schema().trim() : "public");
        config.put("remark", StringUtils.hasText(request.remark()) ? request.remark().trim() : "");
        return config;
    }

    private void writeHealthLog(UUID dataSourceId, DataSourceTestResponse result, String safeTargetJson) {
        jdbcTemplate.update("""
                INSERT INTO platform.data_source_health_log
                    (data_source_id, result, latency_ms, error_code, error_summary, trace_id,
                     test_target_json, created_by, updated_by)
                VALUES
                    (:dataSourceId, :result, :latencyMs, :errorCode, :errorSummary, :traceId,
                     CAST(:testTargetJson AS jsonb), :operator, :operator)
                """, new MapSqlParameterSource()
                .addValue("dataSourceId", dataSourceId)
                .addValue("result", result.success() ? "SUCCESS" : "FAILED")
                .addValue("latencyMs", result.latencyMs())
                .addValue("errorCode", result.success() ? null : result.code())
                .addValue("errorSummary", result.errorSummary())
                .addValue("traceId", TraceIdContext.currentTraceId())
                .addValue("testTargetJson", safeTargetJson)
                .addValue("operator", SYSTEM_OPERATOR));
    }

    private void writeAudit(String action, String resourceId, String changeSummary) {
        jdbcTemplate.update("""
                INSERT INTO platform.audit_log
                    (operator_id, action, resource_type, resource_id, change_summary, result,
                     trace_id, created_by, updated_by)
                VALUES
                    (:operator, :action, 'DATA_SOURCE', :resourceId, :changeSummary, 'SUCCESS',
                     :traceId, :operator, :operator)
                """, new MapSqlParameterSource()
                .addValue("operator", SYSTEM_OPERATOR)
                .addValue("action", action)
                .addValue("resourceId", resourceId)
                .addValue("changeSummary", changeSummary)
                .addValue("traceId", TraceIdContext.currentTraceId()));
    }

    private String safeTargetJson(Map<String, Object> config) {
        Map<String, Object> safe = new HashMap<>();
        safe.put("type", DataSourceType.POSTGRESQL.name());
        safe.put("host", config.get("host"));
        safe.put("port", config.get("port"));
        safe.put("database", config.get("database"));
        safe.put("schema", config.get("schema"));
        return writeJson(safe);
    }

    private String safeTargetJson(DataSourceTestRequest request) {
        Map<String, Object> safe = new HashMap<>();
        safe.put("type", DataSourceType.POSTGRESQL.name());
        safe.put("host", request.host());
        safe.put("port", request.port());
        safe.put("database", request.database());
        safe.put("schema", request.schema());
        return writeJson(safe);
    }

    private String buildCode(String name, UUID id) {
        String normalized = name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (!StringUtils.hasText(normalized)) {
            normalized = "data_source";
        }
        if (normalized.length() > 50) {
            normalized = normalized.substring(0, 50);
        }
        return "ds_" + normalized + "_" + id.toString().substring(0, 8);
    }

    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception exception) {
            throw new IllegalStateException("数据源配置解析失败");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("数据源配置序列化失败");
        }
    }

    private String stringConfig(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Integer intConfig(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null ? null : Integer.parseInt(String.valueOf(value));
    }

    private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }

    private record InternalDataSource(
            UUID id,
            String code,
            String name,
            String type,
            String environment,
            String status,
            Map<String, Object> config,
            String secretRef,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime lastTestTime
    ) {
        private DataSourceResponse toResponse() {
            return new DataSourceResponse(
                    id,
                    code,
                    name,
                    type,
                    environment,
                    string(config, "host"),
                    integer(config, "port"),
                    string(config, "database"),
                    string(config, "username"),
                    string(config, "schema"),
                    string(config, "remark"),
                    status,
                    DataSourceStatus.ENABLED.name().equals(status),
                    lastTestTime,
                    createdAt,
                    updatedAt);
        }

        private static String string(Map<String, Object> config, String key) {
            Object value = config.get(key);
            return value == null ? null : String.valueOf(value);
        }

        private static Integer integer(Map<String, Object> config, String key) {
            Object value = config.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
            return value == null ? null : Integer.parseInt(String.valueOf(value));
        }
    }
}
