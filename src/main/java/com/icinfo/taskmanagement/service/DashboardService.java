package com.icinfo.taskmanagement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icinfo.taskmanagement.dto.DashboardStatusChartResponse;
import com.icinfo.taskmanagement.dto.DashboardStatusChartResponse.StatusChartItem;
import com.icinfo.taskmanagement.dto.DashboardSummaryResponse;
import com.icinfo.taskmanagement.dto.TaskListItemResponse;
import com.icinfo.taskmanagement.entity.Task;
import com.icinfo.taskmanagement.entity.TaskStatus;
import com.icinfo.taskmanagement.mapper.TaskMapper;
import com.icinfo.taskmanagement.security.CurrentUser;
import com.icinfo.taskmanagement.security.CurrentUserContext;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private static final int UPCOMING_DAYS = 7;

    private final TaskMapper taskMapper;

    private final TaskVisibilityService taskVisibilityService;

    public DashboardService(TaskMapper taskMapper, TaskVisibilityService taskVisibilityService) {
        this.taskMapper = taskMapper;
        this.taskVisibilityService = taskVisibilityService;
    }

    public DashboardSummaryResponse getSummary() {
        long todoCount = countByStatus(TaskStatus.TODO);
        long inProgressCount = countByStatus(TaskStatus.IN_PROGRESS);
        long doneCount = countByStatus(TaskStatus.DONE);
        long totalCount = todoCount + inProgressCount + doneCount;
        return new DashboardSummaryResponse(
                todoCount,
                inProgressCount,
                doneCount,
                totalCount,
                completionRate(doneCount, totalCount));
    }

    public DashboardStatusChartResponse getStatusChart() {
        DashboardSummaryResponse summary = getSummary();
        return new DashboardStatusChartResponse(
                List.of("待办", "进行中", "已完成"),
                List.of(
                        new StatusChartItem("待办", TaskStatus.TODO, summary.getTodoCount()),
                        new StatusChartItem("进行中", TaskStatus.IN_PROGRESS, summary.getInProgressCount()),
                        new StatusChartItem("已完成", TaskStatus.DONE, summary.getDoneCount())));
    }

    public List<TaskListItemResponse> listUpcomingTasks() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<Task> wrapper = visibleQuery()
                .ne(Task::getStatus, TaskStatus.DONE)
                .ge(Task::getDueDate, today)
                .le(Task::getDueDate, today.plusDays(UPCOMING_DAYS))
                .orderByAsc(Task::getDueDate)
                .orderByDesc(Task::getId);
        return taskMapper.selectList(wrapper).stream()
                .map(TaskListItemResponse::from)
                .toList();
    }

    public List<TaskListItemResponse> listOverdueTasks() {
        LambdaQueryWrapper<Task> wrapper = visibleQuery()
                .ne(Task::getStatus, TaskStatus.DONE)
                .lt(Task::getDueDate, LocalDate.now())
                .orderByAsc(Task::getDueDate)
                .orderByDesc(Task::getId);
        return taskMapper.selectList(wrapper).stream()
                .map(TaskListItemResponse::from)
                .toList();
    }

    private long countByStatus(TaskStatus status) {
        return taskMapper.selectCount(visibleQuery().eq(Task::getStatus, status));
    }

    private LambdaQueryWrapper<Task> visibleQuery() {
        CurrentUser currentUser = CurrentUserContext.get();
        return taskVisibilityService.buildVisibleTaskQuery(currentUser);
    }

    private double completionRate(long doneCount, long totalCount) {
        if (totalCount == 0) {
            return 0;
        }
        return Math.round(doneCount * 10000.0 / totalCount) / 100.0;
    }
}
