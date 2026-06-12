package com.waterdashboard.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.waterdashboard.common.trace.TraceIdContext;
import com.waterdashboard.dashboard.dto.DashboardDraftRequest;
import com.waterdashboard.dashboard.dto.DashboardDraftResponse;
import com.waterdashboard.dashboard.dto.DashboardListResponse;
import com.waterdashboard.dashboard.dto.DashboardRequest;
import com.waterdashboard.dashboard.dto.DashboardResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DashboardService {

    private static final String SYSTEM_OPERATOR = "system";
    private static final Pattern DASHBOARD_CODE_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]{1,63}$");
    private static final List<String> DANGEROUS_TEXT = List.of(
            "<script", "</script", "javascript:", "vbscript:", "onerror=", "onload=", "onclick=");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DashboardService(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public DashboardListResponse list(String name, String dashboardCode, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, dashboard_code, name, status, description, screen_width, screen_height,
                       background_config_json::text AS background_config_json,
                       theme_config_json::text AS theme_config_json,
                       current_published_version_id, created_at, updated_at
                FROM platform.dashboard
                WHERE 1 = 1
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (StringUtils.hasText(name)) {
            sql.append(" AND name ILIKE :name");
            params.addValue("name", "%" + name.trim() + "%");
        }
        if (StringUtils.hasText(dashboardCode)) {
            sql.append(" AND dashboard_code ILIKE :dashboardCode");
            params.addValue("dashboardCode", "%" + dashboardCode.trim() + "%");
        }
        if (StringUtils.hasText(status)) {
            sql.append(" AND status = :status");
            params.addValue("status", status.trim().toUpperCase(Locale.ROOT));
        }

        sql.append(" ORDER BY updated_at DESC, created_at DESC");
        List<DashboardResponse> items = jdbcTemplate.query(sql.toString(), params, this::mapDashboard);
        return new DashboardListResponse(items, items.size());
    }

    public DashboardResponse get(UUID id) {
        return loadDashboard(id);
    }

    @Transactional
    public DashboardResponse create(DashboardRequest request) {
        validateDashboardRequest(request);
        UUID id = UUID.randomUUID();
        String status = Boolean.TRUE.equals(request.enabled())
                ? DashboardStatus.ENABLED.name()
                : DashboardStatus.DISABLED.name();
        try {
            jdbcTemplate.update("""
                    INSERT INTO platform.dashboard
                        (id, dashboard_code, name, status, description, screen_width, screen_height,
                         background_config_json, theme_config_json, created_by, updated_by)
                    VALUES
                        (:id, :dashboardCode, :name, :status, :description, :screenWidth, :screenHeight,
                         CAST(:backgroundConfigJson AS jsonb), CAST(:themeConfigJson AS jsonb), :operator, :operator)
                    """, dashboardParams(id, request, status));
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("大屏编码已存在");
        }

        ObjectNode emptyDraft = defaultDraftConfig(
                defaultInt(request.screenWidth(), 1920),
                defaultInt(request.screenHeight(), 1080),
                normalizeObject(request.backgroundConfigJson(), defaultBackgroundConfig()),
                normalizeObject(request.themeConfigJson(), defaultThemeConfig()));
        jdbcTemplate.update("""
                INSERT INTO platform.dashboard_draft
                    (dashboard_id, revision, schema_version, config_json, created_by, updated_by)
                VALUES
                    (:dashboardId, 1, '1.0', CAST(:configJson AS jsonb), :operator, :operator)
                """, new MapSqlParameterSource()
                .addValue("dashboardId", id)
                .addValue("configJson", writeJson(emptyDraft))
                .addValue("operator", SYSTEM_OPERATOR));

        writeAudit("DASHBOARD_CREATE", id.toString(), "创建大屏：" + request.dashboardCode().trim());
        writeAudit("DASHBOARD_DRAFT_CREATE", id.toString(), "初始化大屏空草稿：" + request.dashboardCode().trim());
        return loadDashboard(id);
    }

    @Transactional
    public DashboardResponse update(UUID id, DashboardRequest request) {
        validateDashboardRequest(request);
        loadDashboard(id);
        try {
            jdbcTemplate.update("""
                    UPDATE platform.dashboard
                    SET dashboard_code = :dashboardCode,
                        name = :name,
                        status = :status,
                        description = :description,
                        screen_width = :screenWidth,
                        screen_height = :screenHeight,
                        background_config_json = CAST(:backgroundConfigJson AS jsonb),
                        theme_config_json = CAST(:themeConfigJson AS jsonb),
                        updated_by = :operator,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = :id
                    """, dashboardParams(id, request, Boolean.TRUE.equals(request.enabled())
                    ? DashboardStatus.ENABLED.name()
                    : DashboardStatus.DISABLED.name()));
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("大屏编码已存在");
        }
        writeAudit("DASHBOARD_UPDATE", id.toString(), "修改大屏：" + request.dashboardCode().trim());
        return loadDashboard(id);
    }

    @Transactional
    public DashboardResponse enable(UUID id) {
        return changeStatus(id, DashboardStatus.ENABLED);
    }

    @Transactional
    public DashboardResponse disable(UUID id) {
        return changeStatus(id, DashboardStatus.DISABLED);
    }

    public DashboardDraftResponse getDraft(UUID dashboardId) {
        loadDashboard(dashboardId);
        return loadDraft(dashboardId);
    }

    @Transactional
    public DashboardDraftResponse saveDraft(UUID dashboardId, DashboardDraftRequest request) {
        loadDashboard(dashboardId);
        if (request == null || request.configJson() == null || !request.configJson().isObject()) {
            throw new IllegalArgumentException("草稿 configJson 必须是 JSON 对象");
        }
        validateDraftConfig(request.configJson());
        jdbcTemplate.update("""
                UPDATE platform.dashboard_draft
                SET revision = revision + 1,
                    config_json = CAST(:configJson AS jsonb),
                    updated_by = :operator,
                    updated_at = CURRENT_TIMESTAMP
                WHERE dashboard_id = :dashboardId
                """, new MapSqlParameterSource()
                .addValue("dashboardId", dashboardId)
                .addValue("configJson", writeJson(request.configJson()))
                .addValue("operator", SYSTEM_OPERATOR));
        writeAudit("DASHBOARD_DRAFT_SAVE", dashboardId.toString(), "保存大屏草稿");
        return loadDraft(dashboardId);
    }

    private DashboardResponse changeStatus(UUID id, DashboardStatus status) {
        DashboardResponse dashboard = loadDashboard(id);
        jdbcTemplate.update("""
                UPDATE platform.dashboard
                SET status = :status,
                    updated_by = :operator,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", status.name())
                .addValue("operator", SYSTEM_OPERATOR));
        writeAudit("DASHBOARD_" + status.name(), id.toString(),
                (status == DashboardStatus.ENABLED ? "启用大屏：" : "停用大屏：") + dashboard.dashboardCode());
        return loadDashboard(id);
    }

    private void validateDashboardRequest(DashboardRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("大屏请求不能为空");
        }
        if (!StringUtils.hasText(request.dashboardCode())
                || !DASHBOARD_CODE_PATTERN.matcher(request.dashboardCode().trim()).matches()) {
            throw new IllegalArgumentException("大屏编码必须以字母开头，只能包含字母、数字、下划线和中划线，长度 2-64");
        }
        if (!StringUtils.hasText(request.name())) {
            throw new IllegalArgumentException("大屏名称不能为空");
        }
        int screenWidth = defaultInt(request.screenWidth(), 1920);
        int screenHeight = defaultInt(request.screenHeight(), 1080);
        if (screenWidth < 320 || screenWidth > 16384 || screenHeight < 240 || screenHeight > 16384) {
            throw new IllegalArgumentException("大屏尺寸不合法");
        }
        validateText(request.dashboardCode());
        validateText(request.name());
        validateText(request.description());
        validateJsonObject(normalizeObject(request.backgroundConfigJson(), defaultBackgroundConfig()), "backgroundConfigJson");
        validateJsonObject(normalizeObject(request.themeConfigJson(), defaultThemeConfig()), "themeConfigJson");
    }

    private void validateDraftConfig(JsonNode config) {
        validateJsonObject(config, "configJson");
        JsonNode schemaVersion = config.get("schemaVersion");
        if (schemaVersion == null || !schemaVersion.isTextual() || !StringUtils.hasText(schemaVersion.asText())) {
            throw new IllegalArgumentException("草稿必须包含 schemaVersion");
        }
        JsonNode canvas = config.get("canvas");
        if (canvas == null || !canvas.isObject()) {
            throw new IllegalArgumentException("草稿必须包含 canvas 对象");
        }
        if (!config.has("cards") || !config.get("cards").isArray()) {
            throw new IllegalArgumentException("草稿必须包含 cards 数组");
        }
        if (!config.has("interactions") || !config.get("interactions").isArray()) {
            throw new IllegalArgumentException("草稿必须包含 interactions 数组");
        }
        JsonNode aiContext = config.get("aiContext");
        if (aiContext == null || !aiContext.isObject()) {
            throw new IllegalArgumentException("草稿必须包含 aiContext 对象");
        }
    }

    private void validateJsonObject(JsonNode node, String fieldName) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException(fieldName + " 必须是 JSON 对象");
        }
        scanDangerousJson(node);
    }

    private void scanDangerousJson(JsonNode node) {
        if (node.isTextual()) {
            validateText(node.asText());
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                validateText(field.getKey());
                scanDangerousJson(field.getValue());
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                scanDangerousJson(item);
            }
        }
    }

    private void validateText(String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        for (String dangerous : DANGEROUS_TEXT) {
            if (lower.contains(dangerous)) {
                throw new IllegalArgumentException("配置内容包含不允许的脚本片段");
            }
        }
    }

    private MapSqlParameterSource dashboardParams(UUID id, DashboardRequest request, String status) {
        JsonNode background = normalizeObject(request.backgroundConfigJson(), defaultBackgroundConfig());
        JsonNode theme = normalizeObject(request.themeConfigJson(), defaultThemeConfig());
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("dashboardCode", request.dashboardCode().trim())
                .addValue("name", request.name().trim())
                .addValue("status", status)
                .addValue("description", blankToNull(request.description()))
                .addValue("screenWidth", defaultInt(request.screenWidth(), 1920))
                .addValue("screenHeight", defaultInt(request.screenHeight(), 1080))
                .addValue("backgroundConfigJson", writeJson(background))
                .addValue("themeConfigJson", writeJson(theme))
                .addValue("operator", SYSTEM_OPERATOR);
    }

    private DashboardResponse loadDashboard(UUID id) {
        return jdbcTemplate.queryForObject("""
                SELECT id, dashboard_code, name, status, description, screen_width, screen_height,
                       background_config_json::text AS background_config_json,
                       theme_config_json::text AS theme_config_json,
                       current_published_version_id, created_at, updated_at
                FROM platform.dashboard
                WHERE id = :id
                """, new MapSqlParameterSource("id", id), this::mapDashboard);
    }

    private DashboardDraftResponse loadDraft(UUID dashboardId) {
        return jdbcTemplate.queryForObject("""
                SELECT id, dashboard_id, revision, schema_version, config_json::text AS config_json,
                       updated_by, created_at, updated_at
                FROM platform.dashboard_draft
                WHERE dashboard_id = :dashboardId
                """, new MapSqlParameterSource("dashboardId", dashboardId), this::mapDraft);
    }

    private DashboardResponse mapDashboard(ResultSet rs, int rowNum) throws SQLException {
        String status = rs.getString("status");
        return new DashboardResponse(
                rs.getObject("id", UUID.class),
                rs.getString("dashboard_code"),
                rs.getString("name"),
                status,
                DashboardStatus.ENABLED.name().equals(status),
                rs.getString("description"),
                rs.getInt("screen_width"),
                rs.getInt("screen_height"),
                readJson(rs.getString("background_config_json")),
                readJson(rs.getString("theme_config_json")),
                rs.getObject("current_published_version_id", UUID.class),
                toOffsetDateTime(rs.getTimestamp("created_at")),
                toOffsetDateTime(rs.getTimestamp("updated_at")));
    }

    private DashboardDraftResponse mapDraft(ResultSet rs, int rowNum) throws SQLException {
        return new DashboardDraftResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("dashboard_id", UUID.class),
                rs.getLong("revision"),
                rs.getString("schema_version"),
                readJson(rs.getString("config_json")),
                rs.getString("updated_by"),
                toOffsetDateTime(rs.getTimestamp("created_at")),
                toOffsetDateTime(rs.getTimestamp("updated_at")));
    }

    private ObjectNode defaultDraftConfig(int width, int height, JsonNode backgroundConfig, JsonNode themeConfig) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("schemaVersion", "1.0");

        ObjectNode canvas = objectMapper.createObjectNode();
        canvas.put("width", width);
        canvas.put("height", height);
        canvas.put("gridSize", 8);
        canvas.set("background", backgroundConfig);
        root.set("canvas", canvas);

        root.set("theme", themeConfig);
        ArrayNode cards = objectMapper.createArrayNode();
        root.set("cards", cards);
        root.set("interactions", objectMapper.createArrayNode());

        ObjectNode aiContext = objectMapper.createObjectNode();
        aiContext.put("enabled", false);
        aiContext.put("manifestReserved", true);
        root.set("aiContext", aiContext);
        return root;
    }

    private ObjectNode defaultBackgroundConfig() {
        ObjectNode background = objectMapper.createObjectNode();
        background.put("type", "color");
        background.put("value", "#061A2E");
        return background;
    }

    private ObjectNode defaultThemeConfig() {
        ObjectNode theme = objectMapper.createObjectNode();
        theme.put("mode", "dark");
        theme.put("primaryColor", "#00D6FF");
        return theme;
    }

    private JsonNode normalizeObject(JsonNode node, JsonNode defaultValue) {
        return node == null || node.isNull() ? defaultValue : node;
    }

    private void writeAudit(String action, String resourceId, String changeSummary) {
        jdbcTemplate.update("""
                INSERT INTO platform.audit_log
                    (operator_id, action, resource_type, resource_id, change_summary, result,
                     trace_id, created_by, updated_by)
                VALUES
                    (:operator, :action, 'DASHBOARD', :resourceId, :changeSummary, 'SUCCESS',
                     :traceId, :operator, :operator)
                """, new MapSqlParameterSource()
                .addValue("operator", SYSTEM_OPERATOR)
                .addValue("action", action)
                .addValue("resourceId", resourceId)
                .addValue("changeSummary", changeSummary)
                .addValue("traceId", TraceIdContext.currentTraceId()));
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new IllegalStateException("大屏 JSON 解析失败");
        }
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception exception) {
            throw new IllegalStateException("大屏 JSON 序列化失败");
        }
    }

    private int defaultInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }
}
