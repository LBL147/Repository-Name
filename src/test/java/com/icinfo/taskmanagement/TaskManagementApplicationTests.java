package com.icinfo.taskmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class TaskManagementApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUpUsersTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    username VARCHAR(64) NOT NULL,
                    password VARCHAR(128) NOT NULL,
                    display_name VARCHAR(64) NOT NULL,
                    role VARCHAR(16) NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_users_username (username),
                    KEY idx_users_role (role)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.update("""
                INSERT INTO users (username, password, display_name, role)
                VALUES (?, ?, ?, ?), (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    password = VALUES(password),
                    display_name = VALUES(display_name),
                    role = VALUES(role)
                """,
                "mentor_mock",
                "236977126d6375b9fa5f7ec7d7d7055cf36741c990d9c788f68a8427b08cdf08",
                "Mock Mentor",
                "MENTOR",
                "intern_mock",
                "534d9b45e4168ad5e7ab39ddde0387982ec6a2a18b992f62738b23fcde72f7e7",
                "Mock Intern",
                "INTERN");
    }

    @Test
    void healthEndpointReturnsSuccessResponse() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void databaseConnectionIsAvailable() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(2)).isTrue();
            assertThat(connection.getCatalog()).isEqualTo("icinfo_task_management");
        }
    }

    @Test
    void loginReturnsTokenAndUserWithoutPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "mentor_mock",
                                  "password": "mentor123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("mentor_mock"))
                .andExpect(jsonPath("$.data.user.role").value("MENTOR"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist());
    }

    @Test
    void mockLoginByRoleReturnsInternToken() throws Exception {
        mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "INTERN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("intern_mock"))
                .andExpect(jsonPath("$.data.user.role").value("INTERN"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist());
    }

    @Test
    void currentUserEndpointReturnsUserFromToken() throws Exception {
        String token = loginAndExtractToken("mentor_mock", "mentor123");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("mentor_mock"))
                .andExpect(jsonPath("$.data.displayName").value("Mock Mentor"))
                .andExpect(jsonPath("$.data.role").value("MENTOR"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void currentUserEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "mentor_mock",
                                  "password": "password123",
                                  "displayName": "Duplicate Mentor",
                                  "role": "MENTOR"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void authEndpointsAllowCorsPreflight() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private String loginAndExtractToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return root.path("data").path("token").asText();
    }
}
