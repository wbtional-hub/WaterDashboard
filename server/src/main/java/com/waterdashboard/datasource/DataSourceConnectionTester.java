package com.waterdashboard.datasource;

import com.waterdashboard.common.trace.TraceIdContext;
import com.waterdashboard.datasource.dto.DataSourceTestRequest;
import com.waterdashboard.datasource.dto.DataSourceTestResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DataSourceConnectionTester {

    private static final int TIMEOUT_SECONDS = 5;
    private static final Pattern JDBC_URL_PATTERN = Pattern.compile("jdbc:postgresql://[^\\s]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i)(password|pwd)\\s*=\\s*[^\\s;&]+");
    private static final Pattern USER_PATTERN = Pattern.compile("(?i)(user|username)\\s*=\\s*[^\\s;&]+");

    public DataSourceTestResponse test(DataSourceTestRequest request) {
        Instant startedAt = Instant.now();
        try {
            DriverManager.setLoginTimeout(TIMEOUT_SECONDS);
            String schema = StringUtils.hasText(request.schema()) ? request.schema().trim() : "public";
            String jdbcUrl = "jdbc:postgresql://%s:%d/%s?connectTimeout=%d&socketTimeout=%d&currentSchema=%s"
                    .formatted(
                            request.host().trim(),
                            request.port(),
                            request.database().trim(),
                            TIMEOUT_SECONDS,
                            TIMEOUT_SECONDS,
                            schema);

            Properties properties = new Properties();
            properties.setProperty("user", request.username().trim());
            properties.setProperty("password", request.password());
            properties.setProperty("ApplicationName", "water-dashboard-data-source-test");

            try (Connection connection = DriverManager.getConnection(jdbcUrl, properties);
                 Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(TIMEOUT_SECONDS);
                try (ResultSet resultSet = statement.executeQuery("select version()")) {
                    String databaseVersion = resultSet.next() ? simplifyVersion(resultSet.getString(1)) : "PostgreSQL";
                    return new DataSourceTestResponse(
                            true,
                            "OK",
                            "连接测试成功",
                            TraceIdContext.currentTraceId(),
                            Duration.between(startedAt, Instant.now()).toMillis(),
                            "POSTGRESQL",
                            databaseVersion,
                            null);
                }
            }
        } catch (Exception exception) {
            return new DataSourceTestResponse(
                    false,
                    "DATA_SOURCE_CONNECT_FAILED",
                    "连接测试失败",
                    TraceIdContext.currentTraceId(),
                    Duration.between(startedAt, Instant.now()).toMillis(),
                    "POSTGRESQL",
                    null,
                    sanitize(exception.getMessage()));
        }
    }

    private String simplifyVersion(String rawVersion) {
        if (!StringUtils.hasText(rawVersion)) {
            return "PostgreSQL";
        }
        int commaIndex = rawVersion.indexOf(',');
        String shortVersion = commaIndex > 0 ? rawVersion.substring(0, commaIndex) : rawVersion;
        return shortVersion.length() > 180 ? shortVersion.substring(0, 180) : shortVersion;
    }

    private String sanitize(String message) {
        if (!StringUtils.hasText(message)) {
            return "连接失败，请检查主机、端口、数据库、用户名或网络状态";
        }
        String sanitized = JDBC_URL_PATTERN.matcher(message).replaceAll("jdbc:postgresql://***");
        sanitized = PASSWORD_PATTERN.matcher(sanitized).replaceAll("$1=***");
        sanitized = USER_PATTERN.matcher(sanitized).replaceAll("$1=***");
        return sanitized.length() > 300 ? sanitized.substring(0, 300) : sanitized;
    }
}
