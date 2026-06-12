package com.waterdashboard.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waterdashboard.common.trace.TraceIdContext;
import com.waterdashboard.dashboard.dto.DashboardCardPreviewRequest;
import com.waterdashboard.dashboard.dto.DashboardCardPreviewResponse;
import com.waterdashboard.datasource.DataSourceSecretService;
import com.waterdashboard.sqlsecurity.SqlSecurityException;
import com.waterdashboard.sqlsecurity.SqlSecurityService;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DashboardQueryPreviewService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int QUERY_TIMEOUT_SECONDS = 5;
    private static final String SYSTEM_OPERATOR = "system";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final DataSourceSecretService secretService;
    private final SqlSecurityService sqlSecurityService;

    public DashboardQueryPreviewService(
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            DataSourceSecretService secretService,
            SqlSecurityService sqlSecurityService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.secretService = secretService;
        this.sqlSecurityService = sqlSecurityService;
    }

    public DashboardCardPreviewResponse preview(UUID dashboardId, UUID cardId, DashboardCardPreviewRequest request) {
        int limit = normalizeLimit(request == null ? null : request.limit());
        CardBinding binding = null;
        String sqlHash = null;
        Instant start = Instant.now();
        try {
            CardConfig card = loadCard(dashboardId, cardId);
            binding = bindingFrom(card.configJson());
            String sql = sqlSecurityService.validatePreviewSql(binding.sql());
            sqlHash = sqlSecurityService.sqlHash(sql);
            DataSourceConfig dataSource = loadEnabledDataSource(binding.dataSourceId());
            if (!"POSTGRESQL".equalsIgnoreCase(dataSource.type())) {
                throw new DashboardPreviewException("SQL_FORBIDDEN", "第一阶段仅支持 PostgreSQL 数据源预览");
            }
            DashboardCardPreviewResponse response = executePreview(sql, sqlHash, dataSource, limit, start);
            writeQueryLog(dashboardId, cardId, binding.dataSourceId(), sqlHash, response.durationMs(),
                    response.rowCount(), "SUCCESS", null, null);
            return response;
        } catch (SqlSecurityException exception) {
            writeQueryLog(dashboardId, cardId, binding == null ? null : binding.dataSourceId(), sqlHash,
                    durationMs(start), 0, "FORBIDDEN", exception.code(), exception.getMessage());
            throw exception;
        } catch (DashboardPreviewException exception) {
            writeQueryLog(dashboardId, cardId, binding == null ? null : binding.dataSourceId(), sqlHash,
                    durationMs(start), 0, statusForCode(exception.code()), exception.code(), exception.getMessage());
            throw exception;
        } catch (SQLTimeoutException exception) {
            writeQueryLog(dashboardId, cardId, binding == null ? null : binding.dataSourceId(), sqlHash,
                    durationMs(start), 0, "TIMEOUT", "SQL_TIMEOUT", "SQL 预览执行超时");
            throw new DashboardPreviewException("SQL_TIMEOUT", "SQL 预览执行超时，请缩小查询范围");
        } catch (SQLException exception) {
            if ("57014".equals(exception.getSQLState())) {
                writeQueryLog(dashboardId, cardId, binding == null ? null : binding.dataSourceId(), sqlHash,
                        durationMs(start), 0, "TIMEOUT", "SQL_TIMEOUT", "SQL 预览执行超时");
                throw new DashboardPreviewException("SQL_TIMEOUT", "SQL 预览执行超时，请缩小查询范围");
            }
            writeQueryLog(dashboardId, cardId, binding == null ? null : binding.dataSourceId(), sqlHash,
                    durationMs(start), 0, "FAILED", "SQL_EXECUTION_FAILED", "SQL 预览执行失败");
            throw new DashboardPreviewException("SQL_EXECUTION_FAILED", "SQL 预览执行失败，请检查查询语句或数据源权限");
        } catch (RuntimeException exception) {
            if (exception instanceof DashboardPreviewException || exception instanceof SqlSecurityException) {
                throw exception;
            }
            writeQueryLog(dashboardId, cardId, binding == null ? null : binding.dataSourceId(), sqlHash,
                    durationMs(start), 0, "FAILED", "INTERNAL_ERROR", "SQL 预览暂不可用");
            throw exception;
        }
    }

    private DashboardCardPreviewResponse executePreview(
            String sql,
            String sqlHash,
            DataSourceConfig dataSource,
            int limit,
            Instant start) throws SQLException {
        String previewSql = "SELECT * FROM (" + sql + ") preview_q LIMIT ?";
        List<DashboardCardPreviewResponse.Column> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(dataSource.jdbcUrl(), dataSource.username(), dataSource.password())) {
            connection.setReadOnly(true);
            try (PreparedStatement statement = connection.prepareStatement(previewSql)) {
                statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                statement.setMaxRows(limit);
                statement.setInt(1, limit);
                try (ResultSet resultSet = statement.executeQuery()) {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    for (int index = 1; index <= metaData.getColumnCount(); index++) {
                        columns.add(new DashboardCardPreviewResponse.Column(
                                metaData.getColumnLabel(index),
                                metaData.getColumnTypeName(index)));
                    }
                    while (resultSet.next() && rows.size() < limit) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int index = 1; index <= metaData.getColumnCount(); index++) {
                            row.put(metaData.getColumnLabel(index), safeValue(resultSet.getObject(index)));
                        }
                        rows.add(row);
                    }
                }
            }
        }
        return new DashboardCardPreviewResponse(columns, rows, rows.size(), durationMs(start), sqlHash, rows.size() >= limit);
    }

    private Object safeValue(Object value) {
        if ("org.postgresql.util.PGobject".equals(value.getClass().getName())) {
            return String.valueOf(value);
        }
        if (value instanceof byte[] bytes) {
            return "[binary:" + bytes.length + "]";
        }
        return value;
    }

    private CardBinding bindingFrom(JsonNode config) {
        JsonNode dataBinding = config.get("dataBinding");
        if (dataBinding == null || !dataBinding.isObject()) {
            throw new DashboardPreviewException("DATA_BINDING_DISABLED", "卡片未配置数据绑定");
        }
        if (!dataBinding.path("enabled").asBoolean(false)) {
            throw new DashboardPreviewException("DATA_BINDING_DISABLED", "卡片未启用数据绑定");
        }
        String queryType = dataBinding.path("queryType").asText("SQL");
        if (!"SQL".equalsIgnoreCase(queryType)) {
            throw new DashboardPreviewException("SQL_FORBIDDEN", "当前仅支持 SQL 类型数据绑定");
        }
        String dataSourceIdText = dataBinding.path("dataSourceId").asText("");
        if (!StringUtils.hasText(dataSourceIdText)) {
            throw new DashboardPreviewException("DATA_SOURCE_NOT_FOUND", "请选择已启用的数据源");
        }
        UUID dataSourceId;
        try {
            dataSourceId = UUID.fromString(dataSourceIdText);
        } catch (IllegalArgumentException exception) {
            throw new DashboardPreviewException("DATA_SOURCE_NOT_FOUND", "数据源标识不合法");
        }
        String sql = dataBinding.path("sql").asText("");
        if (!StringUtils.hasText(sql)) {
            throw new DashboardPreviewException("SQL_EMPTY", "SQL 不能为空");
        }
        return new CardBinding(dataSourceId, sql);
    }

    private CardConfig loadCard(UUID dashboardId, UUID cardId) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT id, config_json::text AS config_json
                    FROM platform.dashboard_card
                    WHERE dashboard_id = :dashboardId AND id = :cardId
                    """, new MapSqlParameterSource()
                    .addValue("dashboardId", dashboardId)
                    .addValue("cardId", cardId), (rs, rowNum) -> new CardConfig(
                    rs.getObject("id", UUID.class),
                    readJson(rs.getString("config_json"))));
        } catch (EmptyResultDataAccessException exception) {
            throw new DashboardPreviewException("CARD_NOT_FOUND", "卡片不存在");
        }
    }

    private DataSourceConfig loadEnabledDataSource(UUID dataSourceId) {
        try {
            DataSourceConfig dataSource = jdbcTemplate.queryForObject("""
                    SELECT id, type, status, config_json::text AS config_json, secret_ref
                    FROM platform.data_source
                    WHERE id = :id
                    """, new MapSqlParameterSource("id", dataSourceId), (rs, rowNum) -> {
                Map<String, Object> config = readMap(rs.getString("config_json"));
                return new DataSourceConfig(
                        rs.getObject("id", UUID.class),
                        rs.getString("type"),
                        rs.getString("status"),
                        string(config, "host"),
                        integer(config, "port", 5432),
                        string(config, "database"),
                        string(config, "username"),
                        secretService.reveal(rs.getString("secret_ref")));
            });
            if (!"ENABLED".equals(dataSource.status())) {
                throw new DashboardPreviewException("DATA_SOURCE_DISABLED", "数据源未启用");
            }
            return dataSource;
        } catch (EmptyResultDataAccessException exception) {
            throw new DashboardPreviewException("DATA_SOURCE_NOT_FOUND", "数据源不存在");
        }
    }

    private String string(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private Integer integer(Map<String, Object> config, String key, int fallback) {
        Object value = config.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new DashboardPreviewException("INTERNAL_ERROR", "卡片配置读取失败");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String value) {
        try {
            return objectMapper.readValue(value, Map.class);
        } catch (Exception exception) {
            throw new DashboardPreviewException("INTERNAL_ERROR", "数据源配置读取失败");
        }
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }

    private long durationMs(Instant start) {
        return Duration.between(start, Instant.now()).toMillis();
    }

    private String statusForCode(String code) {
        if ("SQL_TIMEOUT".equals(code)) {
            return "TIMEOUT";
        }
        if ("SQL_FORBIDDEN".equals(code) || "DATA_BINDING_DISABLED".equals(code) || "DATA_SOURCE_DISABLED".equals(code)) {
            return "FORBIDDEN";
        }
        return "FAILED";
    }

    private void writeQueryLog(
            UUID dashboardId,
            UUID cardId,
            UUID dataSourceId,
            String sqlHash,
            long durationMs,
            int rowCount,
            String result,
            String errorCode,
            String errorMessage) {
        jdbcTemplate.update("""
                INSERT INTO platform.card_query_log
                    (dashboard_id, card_id, data_source_id, sql_hash, query_type, duration_ms,
                     row_count, result, error_code, error_message, trace_id, created_by, updated_by)
                VALUES
                    (:dashboardId, :cardId, :dataSourceId, :sqlHash, 'SQL_PREVIEW', :durationMs,
                     :rowCount, :result, :errorCode, :errorMessage, :traceId, :operator, :operator)
                """, new MapSqlParameterSource()
                .addValue("dashboardId", dashboardId)
                .addValue("cardId", cardId)
                .addValue("dataSourceId", dataSourceId)
                .addValue("sqlHash", sqlHash)
                .addValue("durationMs", durationMs)
                .addValue("rowCount", rowCount)
                .addValue("result", result)
                .addValue("errorCode", errorCode)
                .addValue("errorMessage", sanitize(errorMessage))
                .addValue("traceId", TraceIdContext.currentTraceId())
                .addValue("operator", SYSTEM_OPERATOR));
    }

    private String sanitize(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        String compact = message.replaceAll("\\s+", " ").trim();
        return compact.length() > 480 ? compact.substring(0, 480) : compact;
    }

    private record CardConfig(UUID id, JsonNode configJson) {
    }

    private record CardBinding(UUID dataSourceId, String sql) {
    }

    private record DataSourceConfig(
            UUID id,
            String type,
            String status,
            String host,
            int port,
            String database,
            String username,
            String password
    ) {
        private String jdbcUrl() {
            return "jdbc:postgresql://" + host + ":" + port + "/" + database;
        }
    }
}
