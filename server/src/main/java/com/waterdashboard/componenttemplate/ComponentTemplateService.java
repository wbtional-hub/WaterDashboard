package com.waterdashboard.componenttemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waterdashboard.common.trace.TraceIdContext;
import com.waterdashboard.componenttemplate.dto.ComponentTemplateDetailResponse;
import com.waterdashboard.componenttemplate.dto.ComponentTemplateListResponse;
import com.waterdashboard.componenttemplate.dto.ComponentTemplateRequest;
import com.waterdashboard.componenttemplate.dto.ComponentTemplateResponse;
import com.waterdashboard.componenttemplate.dto.ComponentTemplateVersionRequest;
import com.waterdashboard.componenttemplate.dto.ComponentTemplateVersionResponse;
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
public class ComponentTemplateService {

    private static final String SYSTEM_OPERATOR = "system";
    private static final Pattern TEMPLATE_CODE_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]{1,63}$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^[0-9A-Za-z][0-9A-Za-z._-]{0,31}$");
    private static final List<String> DANGEROUS_TEXT = List.of(
            "<script", "</script", "javascript:", "vbscript:", "onerror=", "onload=", "onclick=");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ComponentTemplateService(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public ComponentTemplateListResponse list(String name, String templateCode, String category, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, template_code, name, category, render_engine, current_version, status,
                       description, min_width, min_height, default_width, default_height,
                       license_scope, signature, checksum, created_at, updated_at
                FROM platform.component_template
                WHERE 1 = 1
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (StringUtils.hasText(name)) {
            sql.append(" AND name ILIKE :name");
            params.addValue("name", "%" + name.trim() + "%");
        }
        if (StringUtils.hasText(templateCode)) {
            sql.append(" AND template_code ILIKE :templateCode");
            params.addValue("templateCode", "%" + templateCode.trim() + "%");
        }
        if (StringUtils.hasText(category)) {
            sql.append(" AND category = :category");
            params.addValue("category", category.trim());
        }
        if (StringUtils.hasText(status)) {
            sql.append(" AND status = :status");
            params.addValue("status", status.trim().toUpperCase(Locale.ROOT));
        }

        sql.append(" ORDER BY updated_at DESC, created_at DESC");
        List<ComponentTemplateResponse> items = jdbcTemplate.query(sql.toString(), params, this::mapTemplate);
        return new ComponentTemplateListResponse(items, items.size());
    }

    public ComponentTemplateDetailResponse get(UUID id) {
        return new ComponentTemplateDetailResponse(loadTemplate(id), listVersions(id));
    }

    @Transactional
    public ComponentTemplateResponse create(ComponentTemplateRequest request) {
        validateTemplateRequest(request);
        UUID id = UUID.randomUUID();
        String status = Boolean.TRUE.equals(request.enabled())
                ? ComponentTemplateStatus.ENABLED.name()
                : ComponentTemplateStatus.DISABLED.name();
        try {
            jdbcTemplate.update("""
                    INSERT INTO platform.component_template
                        (id, template_code, name, category, render_engine, status,
                         description, min_width, min_height, default_width, default_height,
                         license_scope, signature, checksum, created_by, updated_by)
                    VALUES
                        (:id, :templateCode, :name, :category, :renderEngine, :status,
                         :description, :minWidth, :minHeight, :defaultWidth, :defaultHeight,
                         :licenseScope, :signature, :checksum, :operator, :operator)
                    """, templateParams(id, request, status));
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("组件模板编码已存在");
        }
        writeAudit("COMPONENT_TEMPLATE_CREATE", id.toString(), "创建组件模板：" + request.templateCode().trim());
        return loadTemplate(id);
    }

    @Transactional
    public ComponentTemplateResponse update(UUID id, ComponentTemplateRequest request) {
        validateTemplateRequest(request);
        loadTemplate(id);
        try {
            int updated = jdbcTemplate.update("""
                    UPDATE platform.component_template
                    SET template_code = :templateCode,
                        name = :name,
                        category = :category,
                        render_engine = :renderEngine,
                        status = :status,
                        description = :description,
                        min_width = :minWidth,
                        min_height = :minHeight,
                        default_width = :defaultWidth,
                        default_height = :defaultHeight,
                        license_scope = :licenseScope,
                        signature = :signature,
                        checksum = :checksum,
                        updated_by = :operator,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = :id
                    """, templateParams(id, request, Boolean.TRUE.equals(request.enabled())
                    ? ComponentTemplateStatus.ENABLED.name()
                    : ComponentTemplateStatus.DISABLED.name()));
            if (updated == 0) {
                throw new IllegalArgumentException("组件模板不存在");
            }
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("组件模板编码已存在");
        }
        writeAudit("COMPONENT_TEMPLATE_UPDATE", id.toString(), "修改组件模板：" + request.templateCode().trim());
        return loadTemplate(id);
    }

    @Transactional
    public ComponentTemplateResponse enable(UUID id) {
        return changeStatus(id, ComponentTemplateStatus.ENABLED);
    }

    @Transactional
    public ComponentTemplateResponse disable(UUID id) {
        return changeStatus(id, ComponentTemplateStatus.DISABLED);
    }

    @Transactional
    public ComponentTemplateVersionResponse createVersion(UUID templateId, ComponentTemplateVersionRequest request) {
        validateVersionRequest(request);
        ComponentTemplateResponse template = loadTemplate(templateId);
        UUID versionId = UUID.randomUUID();
        try {
            jdbcTemplate.update("""
                    INSERT INTO platform.component_template_version
                        (id, template_id, version, schema_version, data_contract_json,
                         default_config_json, field_mapping_schema_json, checksum, created_by, updated_by)
                    VALUES
                        (:id, :templateId, :version, '1.0', CAST(:dataContractJson AS jsonb),
                         CAST(:defaultConfigJson AS jsonb), CAST(:fieldMappingSchemaJson AS jsonb),
                         :checksum, :operator, :operator)
                    """, new MapSqlParameterSource()
                    .addValue("id", versionId)
                    .addValue("templateId", templateId)
                    .addValue("version", request.version().trim())
                    .addValue("dataContractJson", writeJson(request.dataContractJson()))
                    .addValue("defaultConfigJson", writeJson(request.defaultConfigJson()))
                    .addValue("fieldMappingSchemaJson", writeJson(request.fieldMappingSchemaJson()))
                    .addValue("checksum", blankToNull(request.checksum()))
                    .addValue("operator", SYSTEM_OPERATOR));
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("该模板版本已存在");
        }

        jdbcTemplate.update("""
                UPDATE platform.component_template
                SET current_version = :version,
                    updated_by = :operator,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :templateId
                """, new MapSqlParameterSource()
                .addValue("templateId", templateId)
                .addValue("version", request.version().trim())
                .addValue("operator", SYSTEM_OPERATOR));

        writeAudit("COMPONENT_TEMPLATE_VERSION_CREATE", versionId.toString(),
                "创建组件模板版本：" + template.templateCode() + "@" + request.version().trim());
        return loadVersion(templateId, versionId);
    }

    public List<ComponentTemplateVersionResponse> listVersions(UUID templateId) {
        loadTemplate(templateId);
        return jdbcTemplate.query("""
                SELECT id, template_id, version, schema_version, data_contract_json::text AS data_contract_json,
                       default_config_json::text AS default_config_json,
                       field_mapping_schema_json::text AS field_mapping_schema_json,
                       checksum, created_by, created_at
                FROM platform.component_template_version
                WHERE template_id = :templateId
                ORDER BY created_at DESC
                """, new MapSqlParameterSource("templateId", templateId), this::mapVersion);
    }

    public ComponentTemplateVersionResponse getVersion(UUID templateId, UUID versionId) {
        loadTemplate(templateId);
        return loadVersion(templateId, versionId);
    }

    private ComponentTemplateResponse changeStatus(UUID id, ComponentTemplateStatus status) {
        ComponentTemplateResponse template = loadTemplate(id);
        jdbcTemplate.update("""
                UPDATE platform.component_template
                SET status = :status,
                    updated_by = :operator,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", status.name())
                .addValue("operator", SYSTEM_OPERATOR));
        writeAudit("COMPONENT_TEMPLATE_" + status.name(), id.toString(),
                (status == ComponentTemplateStatus.ENABLED ? "启用组件模板：" : "停用组件模板：")
                        + template.templateCode());
        return loadTemplate(id);
    }

    private ComponentTemplateResponse loadTemplate(UUID id) {
        return jdbcTemplate.queryForObject("""
                SELECT id, template_code, name, category, render_engine, current_version, status,
                       description, min_width, min_height, default_width, default_height,
                       license_scope, signature, checksum, created_at, updated_at
                FROM platform.component_template
                WHERE id = :id
                """, new MapSqlParameterSource("id", id), this::mapTemplate);
    }

    private ComponentTemplateVersionResponse loadVersion(UUID templateId, UUID versionId) {
        return jdbcTemplate.queryForObject("""
                SELECT id, template_id, version, schema_version, data_contract_json::text AS data_contract_json,
                       default_config_json::text AS default_config_json,
                       field_mapping_schema_json::text AS field_mapping_schema_json,
                       checksum, created_by, created_at
                FROM platform.component_template_version
                WHERE template_id = :templateId AND id = :versionId
                """, new MapSqlParameterSource()
                .addValue("templateId", templateId)
                .addValue("versionId", versionId), this::mapVersion);
    }

    private void validateTemplateRequest(ComponentTemplateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("组件模板请求不能为空");
        }
        if (!StringUtils.hasText(request.templateCode())
                || !TEMPLATE_CODE_PATTERN.matcher(request.templateCode().trim()).matches()) {
            throw new IllegalArgumentException("组件模板编码必须以字母开头，只能包含字母、数字、下划线和中划线，长度 2-64");
        }
        if (!StringUtils.hasText(request.name())) {
            throw new IllegalArgumentException("组件模板名称不能为空");
        }
        if (!StringUtils.hasText(request.category())) {
            throw new IllegalArgumentException("组件模板分类不能为空");
        }
        RenderEngine.parse(request.renderEngine());
        validateText(request.templateCode());
        validateText(request.name());
        validateText(request.category());
        validateText(request.description());
        validateText(request.licenseScope());
        validateText(request.signature());
        validateText(request.checksum());

        int minWidth = defaultInt(request.minWidth(), 1);
        int minHeight = defaultInt(request.minHeight(), 1);
        int defaultWidth = defaultInt(request.defaultWidth(), 4);
        int defaultHeight = defaultInt(request.defaultHeight(), 3);
        if (minWidth < 1 || minHeight < 1 || defaultWidth < minWidth || defaultHeight < minHeight) {
            throw new IllegalArgumentException("组件尺寸配置不合法");
        }
    }

    private void validateVersionRequest(ComponentTemplateVersionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("组件模板版本请求不能为空");
        }
        if (!StringUtils.hasText(request.version()) || !VERSION_PATTERN.matcher(request.version().trim()).matches()) {
            throw new IllegalArgumentException("版本号格式不合法");
        }
        validateJsonObject(request.dataContractJson(), "dataContractJson");
        validateJsonObject(request.defaultConfigJson(), "defaultConfigJson");
        validateJsonObject(request.fieldMappingSchemaJson(), "fieldMappingSchemaJson");
        validateText(request.checksum());
    }

    private void validateJsonObject(JsonNode node, String fieldName) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException(fieldName + " 必须是 JSON 对象");
        }
        scanDangerousJson(node, fieldName);
    }

    private void scanDangerousJson(JsonNode node, String path) {
        if (node.isTextual()) {
            validateText(node.asText());
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                validateText(field.getKey());
                scanDangerousJson(field.getValue(), path + "." + field.getKey());
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                scanDangerousJson(item, path + "[]");
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

    private MapSqlParameterSource templateParams(UUID id, ComponentTemplateRequest request, String status) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("templateCode", request.templateCode().trim())
                .addValue("name", request.name().trim())
                .addValue("category", request.category().trim())
                .addValue("renderEngine", RenderEngine.parse(request.renderEngine()).name())
                .addValue("status", status)
                .addValue("description", blankToNull(request.description()))
                .addValue("minWidth", defaultInt(request.minWidth(), 1))
                .addValue("minHeight", defaultInt(request.minHeight(), 1))
                .addValue("defaultWidth", defaultInt(request.defaultWidth(), 4))
                .addValue("defaultHeight", defaultInt(request.defaultHeight(), 3))
                .addValue("licenseScope", blankToNull(request.licenseScope()))
                .addValue("signature", blankToNull(request.signature()))
                .addValue("checksum", blankToNull(request.checksum()))
                .addValue("operator", SYSTEM_OPERATOR);
    }

    private ComponentTemplateResponse mapTemplate(ResultSet rs, int rowNum) throws SQLException {
        String status = rs.getString("status");
        return new ComponentTemplateResponse(
                rs.getObject("id", UUID.class),
                rs.getString("template_code"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getString("render_engine"),
                rs.getString("current_version"),
                status,
                ComponentTemplateStatus.ENABLED.name().equals(status),
                rs.getString("description"),
                rs.getInt("min_width"),
                rs.getInt("min_height"),
                rs.getInt("default_width"),
                rs.getInt("default_height"),
                rs.getString("license_scope"),
                rs.getString("signature"),
                rs.getString("checksum"),
                toOffsetDateTime(rs.getTimestamp("created_at")),
                toOffsetDateTime(rs.getTimestamp("updated_at")));
    }

    private ComponentTemplateVersionResponse mapVersion(ResultSet rs, int rowNum) throws SQLException {
        return new ComponentTemplateVersionResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("template_id", UUID.class),
                rs.getString("version"),
                rs.getString("schema_version"),
                readJson(rs.getString("data_contract_json")),
                readJson(rs.getString("default_config_json")),
                readJson(rs.getString("field_mapping_schema_json")),
                rs.getString("checksum"),
                rs.getString("created_by"),
                toOffsetDateTime(rs.getTimestamp("created_at")));
    }

    private void writeAudit(String action, String resourceId, String changeSummary) {
        jdbcTemplate.update("""
                INSERT INTO platform.audit_log
                    (operator_id, action, resource_type, resource_id, change_summary, result,
                     trace_id, created_by, updated_by)
                VALUES
                    (:operator, :action, 'COMPONENT_TEMPLATE', :resourceId, :changeSummary, 'SUCCESS',
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
            throw new IllegalStateException("组件模板 JSON 解析失败");
        }
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception exception) {
            throw new IllegalStateException("组件模板 JSON 序列化失败");
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
