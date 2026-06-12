package com.waterdashboard.health;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DatabaseHealthService databaseHealthService;

    @Test
    void healthReturnsUnifiedResponseAndTraceId() throws Exception {
        when(databaseHealthService.check()).thenReturn(DatabaseHealth.up(true));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.databaseStatus").exists())
                .andExpect(jsonPath("$.data.postgresConnected").isBoolean())
                .andExpect(jsonPath("$.data.postgisAvailable").isBoolean())
                .andExpect(jsonPath("$.traceId", not(blankOrNullString())));
    }

    @Test
    void incomingTraceIdIsReused() throws Exception {
        when(databaseHealthService.check()).thenReturn(DatabaseHealth.up(true));

        mockMvc.perform(get("/api/health").header("X-Trace-Id", "known-trace-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "known-trace-id"))
                .andExpect(jsonPath("$.traceId").value("known-trace-id"));
    }

    @Test
    void unexpectedExceptionReturnsUnifiedError() throws Exception {
        mockMvc.perform(get("/api/health/error-demo"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.traceId", not(blankOrNullString())));
    }
}
