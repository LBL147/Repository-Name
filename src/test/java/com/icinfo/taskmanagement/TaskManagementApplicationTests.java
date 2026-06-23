package com.icinfo.taskmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    void setUpDatabase() {
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
                VALUES (?, ?, ?, ?), (?, ?, ?, ?), (?, ?, ?, ?)
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
                "INTERN",
                "intern_other",
                "534d9b45e4168ad5e7ab39ddde0387982ec6a2a18b992f62738b23fcde72f7e7",
                "Other Intern",
                "INTERN");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    title VARCHAR(128) NOT NULL,
                    description TEXT NULL,
                    status VARCHAR(32) NOT NULL DEFAULT 'TODO',
                    priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
                    assignee_id BIGINT NOT NULL,
                    creator_id BIGINT NOT NULL,
                    due_date DATE NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    KEY idx_tasks_status (status),
                    KEY idx_tasks_assignee_id (assignee_id),
                    KEY idx_tasks_creator_id (creator_id),
                    KEY idx_tasks_due_date (due_date)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.update("DELETE FROM tasks");
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

    @Test
    void mentorCanCreateReadUpdateAndDeleteTask() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");
        Long internId = userId("intern_mock");

        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + mentorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Prepare weekly report",
                                  "description": "Collect this week's updates",
                                  "assigneeId": %d,
                                  "dueDate": "2026-07-01"
                                }
                                """.formatted(internId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("Prepare weekly report"))
                .andExpect(jsonPath("$.data.status").value("TODO"))
                .andExpect(jsonPath("$.data.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.data.assigneeId").value(internId))
                .andReturn();
        Long taskId = responseDataId(createResult);

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(taskId))
                .andExpect(jsonPath("$.data.creatorId").value(userId("mentor_mock")));

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + mentorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Prepare final report",
                                  "description": "Collect complete updates",
                                  "status": "IN_PROGRESS",
                                  "priority": "HIGH",
                                  "assigneeId": %d,
                                  "dueDate": "2026-07-02"
                                }
                                """.formatted(internId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("Prepare final report"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"));

        mockMvc.perform(delete("/api/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void mentorCanListAllTasks() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long otherInternId = userId("intern_other");
        createTaskRow("Assigned to mock intern", internId, mentorId);
        createTaskRow("Assigned to other intern", otherInternId, mentorId);

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.records.length()").value(2));
    }

    @Test
    void mentorCanFilterTasksBySingleConditions() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long otherInternId = userId("intern_other");
        Long inProgressTaskId = createTaskRow(
                "Alpha launch",
                "Needs launch checklist",
                "IN_PROGRESS",
                internId,
                mentorId,
                "2026-07-08");
        createTaskRow(
                "Beta review",
                "Contains unique description token",
                "TODO",
                otherInternId,
                mentorId,
                "2026-07-09");

        mockMvc.perform(get("/api/tasks")
                        .param("status", "IN_PROGRESS")
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(inProgressTaskId));

        mockMvc.perform(get("/api/tasks")
                        .param("assigneeId", otherInternId.toString())
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].assigneeId").value(otherInternId));

        mockMvc.perform(get("/api/tasks")
                        .param("dueDateStart", "2026-07-09")
                        .param("dueDateEnd", "2026-07-09")
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("Beta review"));

        mockMvc.perform(get("/api/tasks")
                        .param("keyword", "unique description")
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("Beta review"));
    }

    @Test
    void mentorCanCombineFiltersAndPageResults() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long otherInternId = userId("intern_other");
        createTaskRow(
                "Weekly search report 1",
                "filterable report",
                "TODO",
                internId,
                mentorId,
                "2026-07-10");
        Long secondTaskId = createTaskRow(
                "Weekly search report 2",
                "filterable report",
                "TODO",
                internId,
                mentorId,
                "2026-07-11");
        createTaskRow(
                "Weekly search report 3",
                "filterable report",
                "TODO",
                internId,
                mentorId,
                "2026-07-12");
        createTaskRow(
                "Weekly search report done",
                "filterable report",
                "DONE",
                internId,
                mentorId,
                "2026-07-11");
        createTaskRow(
                "Other assignee search report",
                "filterable report",
                "TODO",
                otherInternId,
                mentorId,
                "2026-07-11");

        mockMvc.perform(get("/api/tasks")
                        .param("status", "TODO")
                        .param("assigneeId", internId.toString())
                        .param("dueDateStart", "2026-07-10")
                        .param("dueDateEnd", "2026-07-12")
                        .param("keyword", "search report")
                        .param("page", "2")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(secondTaskId));
    }

    @Test
    void internCanOnlyListAndMaintainAssignedTasks() throws Exception {
        String internToken = loginAndExtractToken("intern_mock", "intern123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long otherInternId = userId("intern_other");
        Long ownTaskId = createTaskRow("Own assigned task", internId, mentorId);
        createTaskRow("Other assigned task", otherInternId, mentorId);

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + internToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(ownTaskId));

        mockMvc.perform(put("/api/tasks/{id}", ownTaskId)
                        .header("Authorization", "Bearer " + internToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Own assigned task updated",
                                  "description": "Updated by owner",
                                  "status": "DONE",
                                  "priority": "LOW",
                                  "assigneeId": %d,
                                  "dueDate": "2026-07-03"
                                }
                                """.formatted(internId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.data.assigneeId").value(internId));
    }

    @Test
    void internCannotAccessOthersTasksOrDeleteTasks() throws Exception {
        String internToken = loginAndExtractToken("intern_mock", "intern123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long otherInternId = userId("intern_other");
        Long ownTaskId = createTaskRow("Own task", internId, mentorId);
        Long otherTaskId = createTaskRow("Other task", otherInternId, mentorId);

        mockMvc.perform(get("/api/tasks/{id}", otherTaskId)
                        .header("Authorization", "Bearer " + internToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(delete("/api/tasks/{id}", ownTaskId)
                        .header("Authorization", "Bearer " + internToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + internToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Intern create attempt",
                                  "assigneeId": %d
                                }
                                """.formatted(internId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void internFilterCannotEscapeAssignedTaskScope() throws Exception {
        String internToken = loginAndExtractToken("intern_mock", "intern123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long otherInternId = userId("intern_other");
        Long ownTaskId = createTaskRow(
                "Own filtered task",
                "owned boundary keyword",
                "TODO",
                internId,
                mentorId,
                "2026-07-15");
        createTaskRow(
                "Other filtered task",
                "owned boundary keyword",
                "TODO",
                otherInternId,
                mentorId,
                "2026-07-15");

        mockMvc.perform(get("/api/tasks")
                        .param("assigneeId", otherInternId.toString())
                        .param("keyword", "boundary keyword")
                        .header("Authorization", "Bearer " + internToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records.length()").value(0));

        mockMvc.perform(get("/api/tasks")
                        .param("assigneeId", internId.toString())
                        .param("keyword", "boundary keyword")
                        .header("Authorization", "Bearer " + internToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(ownTaskId));
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

    private Long userId(String username) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = ?",
                Long.class,
                username);
    }

    private Long createTaskRow(String title, Long assigneeId, Long creatorId) {
        return createTaskRow(
                title,
                title + " description",
                "TODO",
                assigneeId,
                creatorId,
                "2026-07-01");
    }

    private Long createTaskRow(
            String title,
            String description,
            String status,
            Long assigneeId,
            Long creatorId,
            String dueDate
    ) {
        jdbcTemplate.update("""
                INSERT INTO tasks (title, description, status, priority, assignee_id, creator_id, due_date)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                title,
                description,
                status,
                "MEDIUM",
                assigneeId,
                creatorId,
                dueDate);
        return jdbcTemplate.queryForObject("SELECT id FROM tasks WHERE title = ?", Long.class, title);
    }

    private Long responseDataId(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return root.path("data").path("id").asLong();
    }
}
