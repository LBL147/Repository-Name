package com.icinfo.taskmanagement.controller;

import com.icinfo.taskmanagement.common.ApiResponse;
import com.icinfo.taskmanagement.dto.DashboardStatusChartResponse;
import com.icinfo.taskmanagement.dto.DashboardSummaryResponse;
import com.icinfo.taskmanagement.dto.TaskListItemResponse;
import com.icinfo.taskmanagement.service.DashboardService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> getSummary() {
        return ApiResponse.success(dashboardService.getSummary());
    }

    @GetMapping("/status-chart")
    public ApiResponse<DashboardStatusChartResponse> getStatusChart() {
        return ApiResponse.success(dashboardService.getStatusChart());
    }

    @GetMapping("/upcoming-tasks")
    public ApiResponse<List<TaskListItemResponse>> listUpcomingTasks() {
        return ApiResponse.success(dashboardService.listUpcomingTasks());
    }

    @GetMapping("/overdue-tasks")
    public ApiResponse<List<TaskListItemResponse>> listOverdueTasks() {
        return ApiResponse.success(dashboardService.listOverdueTasks());
    }
}
