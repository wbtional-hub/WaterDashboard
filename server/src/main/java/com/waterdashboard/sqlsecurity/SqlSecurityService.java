package com.waterdashboard.sqlsecurity;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SqlSecurityService {

    private static final List<String> DANGEROUS_KEYWORDS = List.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "TRUNCATE", "CREATE", "GRANT", "REVOKE",
            "EXECUTE", "CALL", "MERGE", "COPY", "VACUUM", "ANALYZE", "LOCK");
    private static final List<String> DANGEROUS_FUNCTIONS = List.of(
            "pg_sleep", "dblink", "lo_import", "lo_export", "copy", "pg_read_file", "pg_write_file",
            "pg_ls_dir", "pg_reload_conf", "pg_terminate_backend", "pg_cancel_backend");

    public String validatePreviewSql(String sql) {
        if (!StringUtils.hasText(sql)) {
            throw new SqlSecurityException("SQL_EMPTY", "SQL 不能为空");
        }
        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (!(upper.startsWith("SELECT") || upper.startsWith("WITH"))) {
            throw new SqlSecurityException("SQL_FORBIDDEN", "SQL 预览只允许 SELECT 或 WITH 查询");
        }
        if (trimmed.contains(";")) {
            throw new SqlSecurityException("SQL_FORBIDDEN", "SQL 预览不允许多语句");
        }
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE).matcher(trimmed).find()) {
                throw new SqlSecurityException("SQL_FORBIDDEN", "SQL 包含不允许的高风险关键字");
            }
        }
        for (String function : DANGEROUS_FUNCTIONS) {
            if (Pattern.compile("\\b" + Pattern.quote(function) + "\\s*\\(", Pattern.CASE_INSENSITIVE)
                    .matcher(trimmed)
                    .find()) {
                throw new SqlSecurityException("SQL_FORBIDDEN", "SQL 包含不允许的高风险函数");
            }
        }
        try {
            Statement statement = CCJSqlParserUtil.parse(trimmed);
            if (!(statement instanceof Select)) {
                throw new SqlSecurityException("SQL_FORBIDDEN", "SQL 预览只允许查询语句");
            }
        } catch (SqlSecurityException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SqlSecurityException("SQL_FORBIDDEN", "SQL 解析失败，请检查是否为单条只读查询");
        }
        return trimmed;
    }

    public String sqlHash(String sql) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(sql.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("SQL 哈希计算失败");
        }
    }
}
