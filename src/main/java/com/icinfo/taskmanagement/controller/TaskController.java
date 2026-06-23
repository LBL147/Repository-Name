package com.icinfo.taskmanagement.controller;

import com.icinfo.taskmanagement.common.ApiResponse;
import com.icinfo.taskmanagement.common.PageResponse;
import com.icinfo.taskmanagement.dto.CreateTaskRequest;
import com.icinfo.taskmanagement.dto.TaskListItemResponse;
import com.icinfo.taskmanagement.dto.TaskQueryRequest;
import com.icinfo.taskmanagement.dto.TaskResponse;
import com.icinfo.taskmanagement.dto.UpdateTaskStatusRequest;
import com.icinfo.taskmanagement.dto.UpdateTaskRequest;
import com.icinfo.taskmanagement.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ApiResponse<PageResponse<TaskListItemResponse>> listTasks(TaskQueryRequest request) {
        return ApiResponse.success(taskService.listTasks(request));
    }

    @PostMapping
    public ApiResponse<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.success(taskService.createTask(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskResponse> getTask(@PathVariable Long id) {
        return ApiResponse.success(taskService.getTask(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return ApiResponse.success(taskService.updateTask(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<TaskResponse> updateTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        return ApiResponse.success(taskService.updateTaskStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ApiResponse.success();
    }
}
