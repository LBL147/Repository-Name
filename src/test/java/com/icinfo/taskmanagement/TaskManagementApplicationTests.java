package com.icinfo.taskmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icinfo.taskmanagement.dto.TaskExportRow;
import com.icinfo.taskmanagement.service.news.ExternalNewsItem;
import com.icinfo.taskmanagement.service.news.NewsFetchException;
import com.icinfo.taskmanagement.service.news.NewsFetchResult;
import com.icinfo.taskmanagement.service.news.NewsFetcher;
import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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

    @MockBean
    private NewsFetcher newsFetcher;

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
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS news_items (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    title VARCHAR(512) NOT NULL,
                    url TEXT NOT NULL,
                    url_hash CHAR(64) NOT NULL,
                    source VARCHAR(128) NOT NULL,
                    keyword VARCHAR(128) NOT NULL,
                    published_at DATETIME NULL,
                    fetched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_news_items_url_hash (url_hash),
                    KEY idx_news_items_keyword (keyword),
                    KEY idx_news_items_published_at (published_at),
                    KEY idx_news_items_fetched_at (fetched_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS task_news (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    task_id BIGINT NOT NULL,
                    news_id BIGINT NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_task_news_task_id_news_id (task_id, news_id),
                    KEY idx_task_news_task_id (task_id),
                    KEY idx_task_news_news_id (news_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.update("DELETE FROM task_news");
        jdbcTemplate.update("DELETE FROM news_items");
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
    void mentorCanListInternsByNameWithoutMentors() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");

        mockMvc.perform(get("/api/users/interns")
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].displayName").value("Mock Intern"))
                .andExpect(jsonPath("$.data[0].role").value("INTERN"))
                .andExpect(jsonPath("$.data[1].displayName").value("Other Intern"))
                .andExpect(jsonPath("$.data[1].role").value("INTERN"));
    }

    @Test
    void internsListRequiresLoginWithChineseMessage() throws Exception {
        mockMvc.perform(get("/api/users/interns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("登录状态已失效，请重新登录"));
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
    void mentorCannotAssignTaskToUnknownUserOrMentor() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long taskId = createTaskRow("Invalid assignee update", internId, mentorId);

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + mentorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Invalid assignee create",
                                  "assigneeId": 999999
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("负责人必须是有效实习生"));

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + mentorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Invalid assignee update",
                                  "status": "TODO",
                                  "priority": "MEDIUM",
                                  "assigneeId": %d
                                }
                                """.formatted(mentorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("负责人必须是有效实习生"));
    }

    @Test
    void taskValidationMessagesAreChinese() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + mentorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "assigneeId": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("请输入任务标题")))
                .andExpect(jsonPath("$.message").value(containsString("请选择负责人")));
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
    void mentorDashboardSummaryCountsAllTasksAndCompletionRate() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long otherInternId = userId("intern_other");
        createTaskRow("Dashboard todo", "todo", "TODO", internId, mentorId, "2026-07-01");
        createTaskRow("Dashboard in progress", "doing", "IN_PROGRESS", otherInternId, mentorId, "2026-07-02");
        createTaskRow("Dashboard done one", "done", "DONE", internId, mentorId, "2026-07-03");
        createTaskRow("Dashboard done two", "done", "DONE", otherInternId, mentorId, "2026-07-04");

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.todoCount").value(1))
                .andExpect(jsonPath("$.data.inProgressCount").value(1))
                .andExpect(jsonPath("$.data.doneCount").value(2))
                .andExpect(jsonPath("$.data.totalCount").value(4))
                .andExpect(jsonPath("$.data.completionRate").value(50.0));
    }

    @Test
    void internDashboardSummaryOnlyCountsAssignedTasks() throws Exception {
        String internToken = loginAndExtractToken("intern_mock", "intern123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long otherInternId = userId("intern_other");
        createTaskRow("Intern visible todo", "todo", "TODO", internId, mentorId, "2026-07-01");
        createTaskRow("Intern visible done", "done", "DONE", internId, mentorId, "2026-07-02");
        createTaskRow("Intern invisible in progress", "doing", "IN_PROGRESS", otherInternId, mentorId, "2026-07-03");

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + internToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.todoCount").value(1))
                .andExpect(jsonPath("$.data.inProgressCount").value(0))
                .andExpect(jsonPath("$.data.doneCount").value(1))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.completionRate").value(50.0));
    }

    @Test
    void dashboardSummaryReturnsZeroRateWhenNoTasksExist() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.todoCount").value(0))
                .andExpect(jsonPath("$.data.inProgressCount").value(0))
                .andExpect(jsonPath("$.data.doneCount").value(0))
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.completionRate").value(0.0));
    }

    @Test
    void dashboardStatusChartReturnsEchartsFriendlyDistribution() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        createTaskRow("Chart todo", "todo", "TODO", internId, mentorId, "2026-07-01");
        createTaskRow("Chart in progress", "doing", "IN_PROGRESS", internId, mentorId, "2026-07-02");
        createTaskRow("Chart done", "done", "DONE", internId, mentorId, "2026-07-03");

        mockMvc.perform(get("/api/dashboard/status-chart")
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.legendData[0]").value("待办"))
                .andExpect(jsonPath("$.data.legendData[1]").value("进行中"))
                .andExpect(jsonPath("$.data.legendData[2]").value("已完成"))
                .andExpect(jsonPath("$.data.seriesData[0].status").value("TODO"))
                .andExpect(jsonPath("$.data.seriesData[0].value").value(1))
                .andExpect(jsonPath("$.data.seriesData[1].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.seriesData[1].value").value(1))
                .andExpect(jsonPath("$.data.seriesData[2].status").value("DONE"))
                .andExpect(jsonPath("$.data.seriesData[2].value").value(1));
    }

    @Test
    void dashboardOverdueTasksOnlyIncludeVisibleUnfinishedPastDueTasks() throws Exception {
        String internToken = loginAndExtractToken("intern_mock", "intern123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long otherInternId = userId("intern_other");
        LocalDate today = LocalDate.now();
        Long overdueTaskId = createTaskRow(
                "Visible overdue",
                "overdue",
                "TODO",
                internId,
                mentorId,
                today.minusDays(1).toString());
        createTaskRow(
                "Completed past due",
                "done",
                "DONE",
                internId,
                mentorId,
                today.minusDays(2).toString());
        createTaskRow(
                "Not yet due",
                "future",
                "IN_PROGRESS",
                internId,
                mentorId,
                today.toString());
        createTaskRow(
                "Invisible overdue",
                "other",
                "TODO",
                otherInternId,
                mentorId,
                today.minusDays(1).toString());

        mockMvc.perform(get("/api/dashboard/overdue-tasks")
                        .header("Authorization", "Bearer " + internToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(overdueTaskId))
                .andExpect(jsonPath("$.data[0].title").value("Visible overdue"))
                .andExpect(jsonPath("$.data[0].status").value("TODO"));
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

    @Test
    void mentorCanExportFilteredTasksWithDownloadHeaders() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long otherInternId = userId("intern_other");
        createTaskRow(
                "Export matching task",
                "contains export keyword",
                "TODO",
                internId,
                mentorId,
                "2026-07-20");
        createTaskRow(
                "Export wrong status",
                "contains export keyword",
                "DONE",
                internId,
                mentorId,
                "2026-07-20");
        createTaskRow(
                "Export wrong assignee",
                "contains export keyword",
                "TODO",
                otherInternId,
                mentorId,
                "2026-07-20");

        MvcResult result = mockMvc.perform(get("/api/tasks/export")
                        .param("status", "TODO")
                        .param("assigneeId", internId.toString())
                        .param("dueDateStart", "2026-07-20")
                        .param("dueDateEnd", "2026-07-20")
                        .param("keyword", "export keyword")
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andReturn();

        assertExcelDownloadHeaders(result);
        List<TaskExportRow> rows = readTaskExportRows(result);
        assertThat(rows).hasSize(1);
        TaskExportRow row = rows.get(0);
        assertThat(row.getTitle()).isEqualTo("Export matching task");
        assertThat(row.getDescription()).isEqualTo("contains export keyword");
        assertThat(row.getStatus()).isEqualTo("TODO");
        assertThat(row.getPriority()).isEqualTo("MEDIUM");
        assertThat(row.getAssignee()).isEqualTo("Mock Intern");
        assertThat(row.getCreator()).isEqualTo("Mock Mentor");
        assertThat(row.getDueDate()).isEqualTo("2026-07-20");
    }

    @Test
    void mentorCanExportAllVisibleTasks() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long otherInternId = userId("intern_other");
        createTaskRow("Mentor export intern task", internId, mentorId);
        createTaskRow("Mentor export other task", otherInternId, mentorId);

        MvcResult result = mockMvc.perform(get("/api/tasks/export")
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(exportTitles(result))
                .containsExactlyInAnyOrder("Mentor export intern task", "Mentor export other task");
    }

    @Test
    void internExportCannotIncludeUnauthorizedTasks() throws Exception {
        String internToken = loginAndExtractToken("intern_mock", "intern123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long otherInternId = userId("intern_other");
        createTaskRow(
                "Intern export own task",
                "shared export boundary",
                "TODO",
                internId,
                mentorId,
                "2026-07-21");
        createTaskRow(
                "Intern export unauthorized task",
                "shared export boundary",
                "TODO",
                otherInternId,
                mentorId,
                "2026-07-21");

        MvcResult ownScopeResult = mockMvc.perform(get("/api/tasks/export")
                        .param("keyword", "export boundary")
                        .header("Authorization", "Bearer " + internToken))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(exportTitles(ownScopeResult)).containsExactly("Intern export own task");

        MvcResult escapeAttemptResult = mockMvc.perform(get("/api/tasks/export")
                        .param("assigneeId", otherInternId.toString())
                        .param("keyword", "export boundary")
                        .header("Authorization", "Bearer " + internToken))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(readTaskExportRows(escapeAttemptResult)).isEmpty();
    }

    @Test
    void mentorCanUpdateTaskStatusFromTodoToInProgress() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long taskId = createTaskRow("Status flow todo task", internId, mentorId);
        jdbcTemplate.update("UPDATE tasks SET updated_at = ? WHERE id = ?", "2026-01-01 00:00:00", taskId);
        LocalDateTime previousUpdatedAt = taskUpdatedAt(taskId);

        mockMvc.perform(patch("/api/tasks/{id}/status", taskId)
                        .header("Authorization", "Bearer " + mentorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());

        assertThat(taskStatus(taskId)).isEqualTo("IN_PROGRESS");
        assertThat(taskUpdatedAt(taskId)).isAfter(previousUpdatedAt);
    }

    @Test
    void internCanUpdateOwnTaskStatusFromInProgressToDone() throws Exception {
        String internToken = loginAndExtractToken("intern_mock", "intern123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long taskId = createTaskRow(
                "Status flow in progress task",
                "Move to done",
                "IN_PROGRESS",
                internId,
                mentorId,
                "2026-07-20");

        mockMvc.perform(patch("/api/tasks/{id}/status", taskId)
                        .header("Authorization", "Bearer " + internToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DONE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("DONE"));

        assertThat(taskStatus(taskId)).isEqualTo("DONE");
    }

    @Test
    void statusUpdateRejectsInvalidStatus() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long taskId = createTaskRow("Status flow invalid task", internId, mentorId);

        mockMvc.perform(patch("/api/tasks/{id}/status", taskId)
                        .header("Authorization", "Bearer " + mentorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ARCHIVED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(taskStatus(taskId)).isEqualTo("TODO");
    }

    @Test
    void statusUpdateRejectsInternUpdatingOthersTask() throws Exception {
        String internToken = loginAndExtractToken("intern_mock", "intern123");
        Long mentorId = userId("mentor_mock");
        Long otherInternId = userId("intern_other");
        Long taskId = createTaskRow("Status flow other assignee task", otherInternId, mentorId);

        mockMvc.perform(patch("/api/tasks/{id}/status", taskId)
                        .header("Authorization", "Bearer " + internToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(taskStatus(taskId)).isEqualTo("TODO");
    }

    @Test
    void statusUpdateRejectsMissingTask() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");

        mockMvc.perform(patch("/api/tasks/{id}/status", 999999L)
                        .header("Authorization", "Bearer " + mentorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DONE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void listNewsSupportsKeywordAndPagination() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");
        Long olderNewsId = createNewsRow(
                "AI policy update",
                "https://example.com/ai-policy",
                "Example News",
                "AI",
                "2026-06-20 10:00:00");
        Long newerNewsId = createNewsRow(
                "AI industry update",
                "https://example.com/ai-industry",
                "Example News",
                "AI",
                "2026-06-21 10:00:00");
        createNewsRow(
                "Cloud release",
                "https://example.com/cloud",
                "Cloud Daily",
                "cloud",
                "2026-06-22 10:00:00");

        mockMvc.perform(get("/api/news")
                        .param("keyword", "AI")
                        .param("page", "1")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(newerNewsId));

        mockMvc.perform(get("/api/news")
                        .param("keyword", "AI")
                        .param("page", "2")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].id").value(olderNewsId));
    }

    @Test
    void refreshNewsCachesFetchedItemsAndDeduplicatesUrl() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");
        when(newsFetcher.fetch("AI")).thenReturn(new NewsFetchResult(
                "GDELT DOC API",
                List.of(
                        new ExternalNewsItem(
                                "AI policy update",
                                "https://example.com/ai-policy",
                                "Example News",
                                LocalDateTime.of(2026, 6, 20, 10, 0)),
                        new ExternalNewsItem(
                                "AI policy duplicate",
                                "https://example.com/ai-policy",
                                "Example News",
                                LocalDateTime.of(2026, 6, 20, 10, 0)),
                        new ExternalNewsItem(
                                "AI industry update",
                                "https://example.com/ai-industry",
                                "Industry News",
                                LocalDateTime.of(2026, 6, 21, 10, 0)))));

        mockMvc.perform(post("/api/news/refresh")
                        .header("Authorization", "Bearer " + mentorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "keyword": "AI"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.source").value("GDELT DOC API"))
                .andExpect(jsonPath("$.data.fetchedCount").value(3))
                .andExpect(jsonPath("$.data.insertedCount").value(2))
                .andExpect(jsonPath("$.data.cacheFallback").value(false))
                .andExpect(jsonPath("$.data.records.length()").value(2));

        assertThat(newsCount()).isEqualTo(2L);

        mockMvc.perform(post("/api/news/refresh")
                        .header("Authorization", "Bearer " + mentorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "keyword": "AI"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.insertedCount").value(0));

        assertThat(newsCount()).isEqualTo(2L);
    }

    @Test
    void refreshNewsReturnsCachedDataWhenExternalSourcesFail() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");
        createNewsRow(
                "Cached AI update",
                "https://example.com/cached-ai",
                "Cached Source",
                "AI",
                "2026-06-20 10:00:00");
        when(newsFetcher.fetch("AI")).thenThrow(new NewsFetchException("down"));

        mockMvc.perform(post("/api/news/refresh")
                        .header("Authorization", "Bearer " + mentorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "keyword": "AI"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.cacheFallback").value(true))
                .andExpect(jsonPath("$.data.message").value("外部资讯暂时不可用，已返回缓存数据"))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("Cached AI update"));
    }

    @Test
    void refreshNewsReturnsFriendlyErrorWhenNoCacheExists() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");
        when(newsFetcher.fetch("AI")).thenThrow(new NewsFetchException("down"));

        mockMvc.perform(post("/api/news/refresh")
                        .header("Authorization", "Bearer " + mentorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "keyword": "AI"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("外部资讯暂时不可用，请稍后重试"));
    }

    @Test
    void listTaskNewsReturnsAssociatedNewsForVisibleTask() throws Exception {
        String internToken = loginAndExtractToken("intern_mock", "intern123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long taskId = createTaskRow("Task with associated news", internId, mentorId);
        Long newsId = createNewsRow(
                "Associated task news",
                "https://example.com/task-associated-news",
                "Example News",
                "Task",
                "2026-06-21 10:00:00");
        createTaskNewsRow(taskId, newsId);

        mockMvc.perform(get("/api/tasks/{id}/news", taskId)
                        .header("Authorization", "Bearer " + internToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].newsId").value(newsId))
                .andExpect(jsonPath("$.data[0].title").value("Associated task news"))
                .andExpect(jsonPath("$.data[0].associatedAt").isNotEmpty());
    }

    @Test
    void refreshTaskNewsUsesTaskTitleWhenKeywordIsMissing() throws Exception {
        String internToken = loginAndExtractToken("intern_mock", "intern123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long taskId = createTaskRow("Supply chain risk", internId, mentorId);
        when(newsFetcher.fetch("Supply chain risk")).thenReturn(new NewsFetchResult(
                "GDELT DOC API",
                List.of(new ExternalNewsItem(
                        "Supply chain risk update",
                        "https://example.com/supply-chain-risk",
                        "Example News",
                        LocalDateTime.of(2026, 6, 21, 10, 0)))));

        mockMvc.perform(post("/api/tasks/{id}/news/refresh", taskId)
                        .header("Authorization", "Bearer " + internToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.keyword").value("Supply chain risk"))
                .andExpect(jsonPath("$.data.refreshSucceeded").value(true))
                .andExpect(jsonPath("$.data.fetchedCount").value(1))
                .andExpect(jsonPath("$.data.insertedCount").value(1))
                .andExpect(jsonPath("$.data.associatedCount").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("Supply chain risk update"));

        assertThat(taskNewsCount(taskId)).isEqualTo(1L);
    }

    @Test
    void refreshTaskNewsPrefersManualKeyword() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long taskId = createTaskRow("Task title keyword", internId, mentorId);
        when(newsFetcher.fetch("manual cloud")).thenReturn(new NewsFetchResult(
                "GDELT DOC API",
                List.of(new ExternalNewsItem(
                        "Manual cloud match",
                        "https://example.com/manual-cloud",
                        "Cloud Daily",
                        LocalDateTime.of(2026, 6, 22, 10, 0)))));

        mockMvc.perform(post("/api/tasks/{id}/news/refresh", taskId)
                        .header("Authorization", "Bearer " + mentorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "keyword": " manual cloud "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.keyword").value("manual cloud"))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].keyword").value("manual cloud"));

        assertThat(taskNewsCount(taskId)).isEqualTo(1L);
    }

    @Test
    void refreshTaskNewsDeduplicatesRepeatedAssociations() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long taskId = createTaskRow("Deduplicate task news", internId, mentorId);
        when(newsFetcher.fetch("Deduplicate task news")).thenReturn(new NewsFetchResult(
                "GDELT DOC API",
                List.of(new ExternalNewsItem(
                        "Deduplicate update",
                        "https://example.com/deduplicate-update",
                        "Example News",
                        LocalDateTime.of(2026, 6, 23, 10, 0)))));

        mockMvc.perform(post("/api/tasks/{id}/news/refresh", taskId)
                        .header("Authorization", "Bearer " + mentorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.associatedCount").value(1));

        mockMvc.perform(post("/api/tasks/{id}/news/refresh", taskId)
                        .header("Authorization", "Bearer " + mentorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.insertedCount").value(0))
                .andExpect(jsonPath("$.data.associatedCount").value(0))
                .andExpect(jsonPath("$.data.records.length()").value(1));

        assertThat(taskNewsCount(taskId)).isEqualTo(1L);
    }

    @Test
    void failedTaskNewsRefreshDoesNotBlockTaskDetails() throws Exception {
        String mentorToken = loginAndExtractToken("mentor_mock", "mentor123");
        Long mentorId = userId("mentor_mock");
        Long internId = userId("intern_mock");
        Long taskId = createTaskRow("Refresh failure task", internId, mentorId);
        when(newsFetcher.fetch("Refresh failure task")).thenThrow(new NewsFetchException("down"));

        mockMvc.perform(post("/api/tasks/{id}/news/refresh", taskId)
                        .header("Authorization", "Bearer " + mentorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.refreshSucceeded").value(false))
                .andExpect(jsonPath("$.data.records.length()").value(0));

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(taskId))
                .andExpect(jsonPath("$.data.title").value("Refresh failure task"));
    }

    @Test
    void taskNewsEndpointsRejectInvisibleTask() throws Exception {
        String internToken = loginAndExtractToken("intern_mock", "intern123");
        Long mentorId = userId("mentor_mock");
        Long otherInternId = userId("intern_other");
        Long otherTaskId = createTaskRow("Invisible task news", otherInternId, mentorId);

        mockMvc.perform(get("/api/tasks/{id}/news", otherTaskId)
                        .header("Authorization", "Bearer " + internToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(post("/api/tasks/{id}/news/refresh", otherTaskId)
                        .header("Authorization", "Bearer " + internToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    private void assertExcelDownloadHeaders(MvcResult result) {
        assertThat(result.getResponse().getContentType())
                .contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment")
                .contains("tasks-export.xlsx");
        byte[] content = result.getResponse().getContentAsByteArray();
        assertThat(content).isNotEmpty();
        assertThat(content[0]).isEqualTo((byte) 'P');
        assertThat(content[1]).isEqualTo((byte) 'K');
    }

    private List<String> exportTitles(MvcResult result) {
        return readTaskExportRows(result).stream()
                .map(TaskExportRow::getTitle)
                .toList();
    }

    private List<TaskExportRow> readTaskExportRows(MvcResult result) {
        return EasyExcel.read(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))
                .head(TaskExportRow.class)
                .sheet()
                .doReadSync();
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

    private String taskStatus(Long taskId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM tasks WHERE id = ?",
                String.class,
                taskId);
    }

    private LocalDateTime taskUpdatedAt(Long taskId) {
        return jdbcTemplate.queryForObject(
                "SELECT updated_at FROM tasks WHERE id = ?",
                LocalDateTime.class,
                taskId);
    }

    private Long createNewsRow(String title, String url, String source, String keyword, String publishedAt) {
        jdbcTemplate.update("""
                INSERT INTO news_items (title, url, url_hash, source, keyword, published_at, fetched_at)
                VALUES (?, ?, SHA2(?, 256), ?, ?, ?, ?)
                """,
                title,
                url,
                url,
                source,
                keyword,
                publishedAt,
                "2026-06-23 10:00:00");
        return jdbcTemplate.queryForObject("SELECT id FROM news_items WHERE url = ?", Long.class, url);
    }

    private Long createTaskNewsRow(Long taskId, Long newsId) {
        jdbcTemplate.update("""
                INSERT INTO task_news (task_id, news_id)
                VALUES (?, ?)
                """,
                taskId,
                newsId);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM task_news WHERE task_id = ? AND news_id = ?",
                Long.class,
                taskId,
                newsId);
    }

    private Long newsCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM news_items", Long.class);
    }

    private Long taskNewsCount(Long taskId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM task_news WHERE task_id = ?",
                Long.class,
                taskId);
    }

    private Long responseDataId(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return root.path("data").path("id").asLong();
    }
}
