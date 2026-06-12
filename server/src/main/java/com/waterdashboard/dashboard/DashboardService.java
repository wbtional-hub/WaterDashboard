package com.waterdashboard.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.waterdashboard.common.trace.TraceIdContext;
import com.waterdashboard.dashboard.dto.DashboardDraftRequest;
import com.waterdashboard.dashboard.dto.DashboardDraftResponse;
import com.waterdashboard.dashboard.dto.DashboardCardRequest;
import com.waterdashboard.dashboard.dto.DashboardCardResponse;
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
import org.springframework.dao.EmptyResultDataAccessException;
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
    private static final List<String> DANGEROUS_SQL_KEYWORDS = List.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "TRUNCATE", "CREATE", "GRANT", "REVOKE", "EXECUTE", "CALL");
    private static final List<String> SENSITIVE_CONFIG_KEYS = List.of(
            "password", "passwd", "pwd", "token", "secret", "connectionstring", "connection_string",
            "privatekey", "private_key", "accesskey", "access_key");

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

    public List<DashboardCardResponse> listCards(UUID dashboardId) {
        loadDashboard(dashboardId);
        return jdbcTemplate.query("""
                SELECT id, dashboard_id, card_code, template_code, enabled, ai_enabled,
                       config_json::text AS config_json, created_at, updated_at
                FROM platform.dashboard_card
                WHERE dashboard_id = :dashboardId
                ORDER BY created_at ASC
                """, new MapSqlParameterSource("dashboardId", dashboardId), this::mapCard);
    }

    @Transactional
    public DashboardCardResponse createCard(UUID dashboardId, DashboardCardRequest request) {
        loadDashboard(dashboardId);
        TemplateInfo template = loadEnabledTemplate(request);
        UUID cardId = UUID.randomUUID();
        String cardCode = StringUtils.hasText(request.cardCode())
                ? request.cardCode().trim()
                : "card_" + cardId.toString().substring(0, 8);
        if (cardCodeExists(dashboardId, cardCode, null)) {
            throw new IllegalArgumentException("同一大屏下卡片编码已存在");
        }
        ObjectNode cardConfig = buildCardConfig(cardId, cardCode, template, request, null);
        validateJsonObject(cardConfig, "card.configJson");

        jdbcTemplate.update("""
                INSERT INTO platform.dashboard_card
                    (id, dashboard_id, card_code, template_code, enabled, ai_enabled,
                     config_json, created_by, updated_by)
                VALUES
                    (:id, :dashboardId, :cardCode, :templateCode, :enabled, :aiEnabled,
                     CAST(:configJson AS jsonb), :operator, :operator)
                """, new MapSqlParameterSource()
                .addValue("id", cardId)
                .addValue("dashboardId", dashboardId)
                .addValue("cardCode", cardCode)
                .addValue("templateCode", template.templateCode())
                .addValue("enabled", cardConfig.get("enabled").asBoolean())
                .addValue("aiEnabled", cardConfig.get("aiEnabled").asBoolean())
                .addValue("configJson", writeJson(cardConfig))
                .addValue("operator", SYSTEM_OPERATOR));
        syncDraftCard(dashboardId, cardConfig, true);
        writeAudit("DASHBOARD_CARD_CREATE", cardId.toString(), "新增草稿卡片：" + cardCode);
        return loadCard(dashboardId, cardId);
    }

    @Transactional
    public DashboardCardResponse updateCard(UUID dashboardId, UUID cardId, DashboardCardRequest request) {
        loadDashboard(dashboardId);
        DashboardCardResponse existing = loadCard(dashboardId, cardId);
        TemplateInfo template = loadEnabledTemplateByCode(existing.templateCode());
        String nextCardCode = StringUtils.hasText(request.cardCode()) ? request.cardCode().trim() : existing.cardCode();
        if (cardCodeExists(dashboardId, nextCardCode, cardId)) {
            throw new IllegalArgumentException("同一大屏下卡片编码已存在");
        }
        ObjectNode currentConfig = (ObjectNode) readJson(writeJson(existing.configJson()));
        ObjectNode nextConfig = buildCardConfig(cardId, nextCardCode, template, request, currentConfig);
        validateJsonObject(nextConfig, "card.configJson");

        jdbcTemplate.update("""
                UPDATE platform.dashboard_card
                SET card_code = :cardCode,
                    enabled = :enabled,
                    ai_enabled = :aiEnabled,
                    config_json = CAST(:configJson AS jsonb),
                    updated_by = :operator,
                    updated_at = CURRENT_TIMESTAMP
                WHERE dashboard_id = :dashboardId AND id = :cardId
                """, new MapSqlParameterSource()
                .addValue("dashboardId", dashboardId)
                .addValue("cardId", cardId)
                .addValue("cardCode", nextCardCode)
                .addValue("enabled", nextConfig.get("enabled").asBoolean())
                .addValue("aiEnabled", nextConfig.get("aiEnabled").asBoolean())
                .addValue("configJson", writeJson(nextConfig))
                .addValue("operator", SYSTEM_OPERATOR));
        syncDraftCard(dashboardId, nextConfig, true);
        writeAudit("DASHBOARD_CARD_UPDATE", cardId.toString(), "修改草稿卡片：" + nextCardCode);
        return loadCard(dashboardId, cardId);
    }

    @Transactional
    public void deleteCard(UUID dashboardId, UUID cardId) {
        loadDashboard(dashboardId);
        DashboardCardResponse existing = loadCard(dashboardId, cardId);
        jdbcTemplate.update("""
                DELETE FROM platform.dashboard_card
                WHERE dashboard_id = :dashboardId AND id = :cardId
                """, new MapSqlParameterSource()
                .addValue("dashboardId", dashboardId)
                .addValue("cardId", cardId));
        ObjectNode cardConfig = objectMapper.createObjectNode();
        cardConfig.put("cardId", cardId.toString());
        syncDraftCard(dashboardId, cardConfig, false);
        writeAudit("DASHBOARD_CARD_DELETE", cardId.toString(), "删除草稿卡片：" + existing.cardCode());
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

    private TemplateInfo loadEnabledTemplate(DashboardCardRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("卡片请求不能为空");
        }
        if (request.templateId() != null) {
            return jdbcTemplate.queryForObject("""
                    SELECT id, template_code, name, render_engine, default_width, default_height
                    FROM platform.component_template
                    WHERE id = :templateId AND status = 'ENABLED'
                    """, new MapSqlParameterSource("templateId", request.templateId()), this::mapTemplateInfo);
        }
        if (StringUtils.hasText(request.templateCode())) {
            return loadEnabledTemplateByCode(request.templateCode().trim());
        }
        throw new IllegalArgumentException("必须指定 templateId 或 templateCode");
    }

    private TemplateInfo loadEnabledTemplateByCode(String templateCode) {
        return jdbcTemplate.queryForObject("""
                SELECT id, template_code, name, render_engine, default_width, default_height
                FROM platform.component_template
                WHERE template_code = :templateCode AND status = 'ENABLED'
                """, new MapSqlParameterSource("templateCode", templateCode), this::mapTemplateInfo);
    }

    private TemplateInfo mapTemplateInfo(ResultSet rs, int rowNum) throws SQLException {
        return new TemplateInfo(
                rs.getObject("id", UUID.class),
                rs.getString("template_code"),
                rs.getString("name"),
                rs.getString("render_engine"),
                rs.getInt("default_width"),
                rs.getInt("default_height"));
    }

    private ObjectNode buildCardConfig(
            UUID cardId,
            String cardCode,
            TemplateInfo template,
            DashboardCardRequest request,
            ObjectNode existing) {
        ObjectNode base = existing == null ? objectMapper.createObjectNode() : existing.deepCopy();
        ObjectNode requestConfig = request.configJson() != null && request.configJson().isObject()
                ? (ObjectNode) request.configJson()
                : objectMapper.createObjectNode();
        scanDangerousJson(requestConfig);

        base.put("cardId", cardId.toString());
        base.put("cardCode", cardCode);
        base.put("title", StringUtils.hasText(request.title())
                ? request.title().trim()
                : textOrDefault(base.get("title"), template.name()));
        base.put("templateCode", template.templateCode());
        base.put("templateId", template.id().toString());
        if (request.templateVersionId() != null) {
            base.put("templateVersionId", request.templateVersionId().toString());
        } else if (!base.has("templateVersionId")) {
            base.putNull("templateVersionId");
        }
        base.put("renderEngine", template.renderEngine());

        ObjectNode layout = base.has("layout") && base.get("layout").isObject()
                ? (ObjectNode) base.get("layout")
                : objectMapper.createObjectNode();
        layout.put("x", defaultInt(request.x(), intOrDefault(layout.get("x"), 40)));
        layout.put("y", defaultInt(request.y(), intOrDefault(layout.get("y"), 40)));
        layout.put("width", defaultInt(request.width(), intOrDefault(layout.get("width"), defaultCardWidth(template))));
        layout.put("height", defaultInt(request.height(), intOrDefault(layout.get("height"), defaultCardHeight(template))));
        base.set("layout", layout);

        base.put("enabled", request.enabled() == null ? booleanOrDefault(base.get("enabled"), true) : request.enabled());
        base.put("aiEnabled", request.aiEnabled() == null ? booleanOrDefault(base.get("aiEnabled"), false) : request.aiEnabled());
        if (!base.has("dataBinding")) {
            ObjectNode dataBinding = objectMapper.createObjectNode();
            dataBinding.put("enabled", false);
            dataBinding.putNull("dataSourceId");
            dataBinding.put("queryType", "SQL");
            dataBinding.put("sql", "");
            dataBinding.set("params", objectMapper.createObjectNode());
            dataBinding.set("fieldMapping", objectMapper.createObjectNode());
            base.set("dataBinding", dataBinding);
        }
        if (!base.has("style")) {
            base.set("style", objectMapper.createObjectNode());
        }
        if (!base.has("refresh")) {
            ObjectNode refresh = objectMapper.createObjectNode();
            refresh.put("enabled", false);
            refresh.put("intervalSeconds", 60);
            base.set("refresh", refresh);
        }
        requestConfig.fields().forEachRemaining(entry -> {
            if (!List.of("cardId", "cardCode", "templateCode", "templateId", "renderEngine").contains(entry.getKey())) {
                base.set(entry.getKey(), entry.getValue());
            }
        });
        normalizeAndValidateCardRuntimeConfig(base);
        return base;
    }

    private void normalizeAndValidateCardRuntimeConfig(ObjectNode base) {
        ObjectNode dataBinding = base.has("dataBinding") && base.get("dataBinding").isObject()
                ? (ObjectNode) base.get("dataBinding")
                : objectMapper.createObjectNode();
        boolean bindingEnabled = booleanOrDefault(dataBinding.get("enabled"), false);
        dataBinding.put("enabled", bindingEnabled);
        dataBinding.put("queryType", "SQL");
        if (!dataBinding.has("params") || !dataBinding.get("params").isObject()) {
            dataBinding.set("params", objectMapper.createObjectNode());
        }
        if (!dataBinding.has("fieldMapping") || !dataBinding.get("fieldMapping").isObject()) {
            dataBinding.set("fieldMapping", objectMapper.createObjectNode());
        }
        String dataSourceId = textOrDefault(dataBinding.get("dataSourceId"), "");
        String sql = textOrDefault(dataBinding.get("sql"), "").trim();
        dataBinding.put("sql", sql);
        if (bindingEnabled) {
            if (!StringUtils.hasText(dataSourceId)) {
                throw new IllegalArgumentException("启用数据绑定时必须选择数据源");
            }
            validateEnabledDataSource(dataSourceId);
            validateConfiguredSql(sql);
        } else {
            if (StringUtils.hasText(dataSourceId)) {
                validateEnabledDataSource(dataSourceId);
            }
            if (StringUtils.hasText(sql)) {
                validateConfiguredSql(sql);
            }
        }
        base.set("dataBinding", dataBinding);

        ObjectNode refresh = base.has("refresh") && base.get("refresh").isObject()
                ? (ObjectNode) base.get("refresh")
                : objectMapper.createObjectNode();
        refresh.put("enabled", booleanOrDefault(refresh.get("enabled"), false));
        int intervalSeconds = intOrDefault(refresh.get("intervalSeconds"), 60);
        if (intervalSeconds < 5 || intervalSeconds > 86400) {
            throw new IllegalArgumentException("刷新间隔必须在 5 到 86400 秒之间");
        }
        refresh.put("intervalSeconds", intervalSeconds);
        base.set("refresh", refresh);
    }

    private void validateEnabledDataSource(String dataSourceId) {
        UUID id;
        try {
            id = UUID.fromString(dataSourceId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("数据源标识不合法");
        }
        String status;
        try {
            status = jdbcTemplate.queryForObject("""
                    SELECT status
                    FROM platform.data_source
                    WHERE id = :id
                    """, new MapSqlParameterSource("id", id), String.class);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("数据源不存在或不可用");
        }
        if (!"ENABLED".equals(status)) {
            throw new IllegalArgumentException("数据源不可用，请选择已启用的数据源");
        }
    }

    private void validateConfiguredSql(String sql) {
        if (!StringUtils.hasText(sql)) {
            throw new IllegalArgumentException("SQL 不能为空");
        }
        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (!(upper.startsWith("SELECT") || upper.startsWith("WITH"))) {
            throw new IllegalArgumentException("卡片 SQL 只允许 SELECT 或 WITH 查询");
        }
        if (trimmed.contains(";")) {
            throw new IllegalArgumentException("卡片 SQL 不允许保存多语句");
        }
        for (String keyword : DANGEROUS_SQL_KEYWORDS) {
            if (Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE).matcher(trimmed).find()) {
                throw new IllegalArgumentException("卡片 SQL 包含不允许的高风险关键字");
            }
        }
    }

    private void syncDraftCard(UUID dashboardId, ObjectNode cardConfig, boolean upsert) {
        DashboardDraftResponse draft = loadDraft(dashboardId);
        ObjectNode config = (ObjectNode) draft.configJson().deepCopy();
        ArrayNode cards = config.has("cards") && config.get("cards").isArray()
                ? (ArrayNode) config.get("cards")
                : objectMapper.createArrayNode();
        String cardId = cardConfig.get("cardId").asText();
        ArrayNode nextCards = objectMapper.createArrayNode();
        boolean replaced = false;
        for (JsonNode card : cards) {
            if (card.has("cardId") && cardId.equals(card.get("cardId").asText())) {
                if (upsert) {
                    nextCards.add(cardConfig);
                    replaced = true;
                }
            } else {
                nextCards.add(card);
            }
        }
        if (upsert && !replaced) {
            nextCards.add(cardConfig);
        }
        config.set("cards", nextCards);
        validateDraftConfig(config);
        jdbcTemplate.update("""
                UPDATE platform.dashboard_draft
                SET revision = revision + 1,
                    config_json = CAST(:configJson AS jsonb),
                    updated_by = :operator,
                    updated_at = CURRENT_TIMESTAMP
                WHERE dashboard_id = :dashboardId
                """, new MapSqlParameterSource()
                .addValue("dashboardId", dashboardId)
                .addValue("configJson", writeJson(config))
                .addValue("operator", SYSTEM_OPERATOR));
    }

    private DashboardCardResponse loadCard(UUID dashboardId, UUID cardId) {
        return jdbcTemplate.queryForObject("""
                SELECT id, dashboard_id, card_code, template_code, enabled, ai_enabled,
                       config_json::text AS config_json, created_at, updated_at
                FROM platform.dashboard_card
                WHERE dashboard_id = :dashboardId AND id = :cardId
                """, new MapSqlParameterSource()
                .addValue("dashboardId", dashboardId)
                .addValue("cardId", cardId), this::mapCard);
    }

    private DashboardCardResponse mapCard(ResultSet rs, int rowNum) throws SQLException {
        JsonNode config = readJson(rs.getString("config_json"));
        JsonNode layout = config.get("layout");
        return new DashboardCardResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("dashboard_id", UUID.class),
                rs.getString("card_code"),
                textOrDefault(config.get("title"), rs.getString("card_code")),
                rs.getString("template_code"),
                config.hasNonNull("templateId") ? UUID.fromString(config.get("templateId").asText()) : null,
                config.hasNonNull("templateVersionId") ? UUID.fromString(config.get("templateVersionId").asText()) : null,
                textOrDefault(config.get("renderEngine"), ""),
                layout == null ? 0 : intOrDefault(layout.get("x"), 0),
                layout == null ? 0 : intOrDefault(layout.get("y"), 0),
                layout == null ? 320 : intOrDefault(layout.get("width"), 320),
                layout == null ? 180 : intOrDefault(layout.get("height"), 180),
                rs.getBoolean("enabled"),
                rs.getBoolean("ai_enabled"),
                config,
                toOffsetDateTime(rs.getTimestamp("created_at")),
                toOffsetDateTime(rs.getTimestamp("updated_at")));
    }

    private boolean cardCodeExists(UUID dashboardId, String cardCode, UUID excludeCardId) {
        String sql = excludeCardId == null
                ? """
                SELECT count(*)
                FROM platform.dashboard_card
                WHERE dashboard_id = :dashboardId AND card_code = :cardCode
                """
                : """
                SELECT count(*)
                FROM platform.dashboard_card
                WHERE dashboard_id = :dashboardId AND card_code = :cardCode AND id <> :excludeCardId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("dashboardId", dashboardId)
                .addValue("cardCode", cardCode);
        if (excludeCardId != null) {
            params.addValue("excludeCardId", excludeCardId);
        }
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
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
        for (JsonNode card : config.get("cards")) {
            if (card.isObject()) {
                validateJsonObject(card, "card");
            }
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
                validateConfigKey(field.getKey());
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

    private void validateConfigKey(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        String normalized = key.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        for (String sensitiveKey : SENSITIVE_CONFIG_KEYS) {
            String normalizedSensitiveKey = sensitiveKey.replace("_", "");
            if (normalized.contains(normalizedSensitiveKey)) {
                throw new IllegalArgumentException("配置内容包含不允许的敏感字段");
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

    private int defaultCardWidth(TemplateInfo template) {
        return template.defaultWidth() > 24 ? template.defaultWidth() : Math.max(320, template.defaultWidth() * 80);
    }

    private int defaultCardHeight(TemplateInfo template) {
        return template.defaultHeight() > 24 ? template.defaultHeight() : Math.max(180, template.defaultHeight() * 60);
    }

    private int intOrDefault(JsonNode node, int defaultValue) {
        return node != null && node.isNumber() ? node.asInt() : defaultValue;
    }

    private boolean booleanOrDefault(JsonNode node, boolean defaultValue) {
        return node != null && node.isBoolean() ? node.asBoolean() : defaultValue;
    }

    private String textOrDefault(JsonNode node, String defaultValue) {
        return node != null && node.isTextual() ? node.asText() : defaultValue;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }

    private record TemplateInfo(
            UUID id,
            String templateCode,
            String name,
            String renderEngine,
            int defaultWidth,
            int defaultHeight
    ) {
    }
}
