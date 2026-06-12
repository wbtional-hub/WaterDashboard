package com.waterdashboard.health;

import com.waterdashboard.common.trace.TraceIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseHealthService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthService.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DatabaseHealth check() {
        try {
            Integer connected = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            Boolean postgisAvailable = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'postgis')",
                    Boolean.class
            );
            return connected != null && connected == 1
                    ? DatabaseHealth.up(Boolean.TRUE.equals(postgisAvailable))
                    : DatabaseHealth.down();
        } catch (Exception exception) {
            log.warn(
                    "Platform database health check failed, traceId={}",
                    TraceIdContext.currentTraceId(),
                    exception
            );
            return DatabaseHealth.down();
        }
    }
}

