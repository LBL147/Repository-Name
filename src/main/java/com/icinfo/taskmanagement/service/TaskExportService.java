package com.icinfo.taskmanagement.service;

import com.alibaba.excel.EasyExcel;
import com.icinfo.taskmanagement.dto.TaskExportRow;
import com.icinfo.taskmanagement.dto.TaskQueryRequest;
import com.icinfo.taskmanagement.entity.Task;
import com.icinfo.taskmanagement.entity.User;
import com.icinfo.taskmanagement.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
public class TaskExportService {

    private static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TaskService taskService;

    private final UserMapper userMapper;

    public TaskExportService(TaskService taskService, UserMapper userMapper) {
        this.taskService = taskService;
        this.userMapper = userMapper;
    }

    public void exportTasks(TaskQueryRequest request, HttpServletResponse response) throws IOException {
        List<Task> tasks = taskService.listVisibleTaskEntities(request);
        Map<Long, String> userNames = loadUserNames(tasks);
        List<TaskExportRow> rows = tasks.stream()
                .map(task -> toRow(task, userNames))
                .toList();

        String filename = "tasks-export.xlsx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(EXCEL_CONTENT_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename);

        EasyExcel.write(response.getOutputStream(), TaskExportRow.class)
                .autoCloseStream(false)
                .sheet("Tasks")
                .doWrite(rows);
    }

    private Map<Long, String> loadUserNames(List<Task> tasks) {
        List<Long> userIds = tasks.stream()
                .flatMap(task -> Stream.of(task.getAssigneeId(), task.getCreatorId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }

        Collection<User> users = userMapper.selectBatchIds(userIds);
        return users.stream()
                .collect(Collectors.toMap(User::getId, this::displayName, (first, second) -> first, HashMap::new));
    }

    private TaskExportRow toRow(Task task, Map<Long, String> userNames) {
        TaskExportRow row = new TaskExportRow();
        row.setTitle(task.getTitle());
        row.setDescription(task.getDescription());
        row.setStatus(task.getStatus() == null ? null : task.getStatus().name());
        row.setPriority(task.getPriority() == null ? null : task.getPriority().name());
        row.setAssignee(userNames.getOrDefault(task.getAssigneeId(), String.valueOf(task.getAssigneeId())));
        row.setCreator(userNames.getOrDefault(task.getCreatorId(), String.valueOf(task.getCreatorId())));
        row.setDueDate(task.getDueDate() == null ? null : task.getDueDate().toString());
        row.setCreatedAt(task.getCreatedAt() == null ? null : task.getCreatedAt().format(DATE_TIME_FORMATTER));
        row.setUpdatedAt(task.getUpdatedAt() == null ? null : task.getUpdatedAt().format(DATE_TIME_FORMATTER));
        return row;
    }

    private String displayName(User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return String.valueOf(user.getId());
    }
}
