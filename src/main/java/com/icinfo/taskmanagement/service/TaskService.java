package com.icinfo.taskmanagement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icinfo.taskmanagement.common.ErrorCode;
import com.icinfo.taskmanagement.dto.CreateTaskRequest;
import com.icinfo.taskmanagement.dto.TaskListItemResponse;
import com.icinfo.taskmanagement.dto.TaskResponse;
import com.icinfo.taskmanagement.dto.UpdateTaskRequest;
import com.icinfo.taskmanagement.entity.Task;
import com.icinfo.taskmanagement.entity.TaskPriority;
import com.icinfo.taskmanagement.entity.TaskStatus;
import com.icinfo.taskmanagement.entity.User;
import com.icinfo.taskmanagement.entity.UserRole;
import com.icinfo.taskmanagement.exception.BusinessException;
import com.icinfo.taskmanagement.mapper.TaskMapper;
import com.icinfo.taskmanagement.mapper.UserMapper;
import com.icinfo.taskmanagement.security.CurrentUser;
import com.icinfo.taskmanagement.security.CurrentUserContext;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskMapper taskMapper;

    private final UserMapper userMapper;

    public TaskService(TaskMapper taskMapper, UserMapper userMapper) {
        this.taskMapper = taskMapper;
        this.userMapper = userMapper;
    }

    public List<TaskListItemResponse> listTasks() {
        CurrentUser currentUser = CurrentUserContext.get();
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .orderByDesc(Task::getCreatedAt)
                .orderByDesc(Task::getId);
        if (!isMentor(currentUser)) {
            wrapper.eq(Task::getAssigneeId, currentUser.getId());
        }
        return taskMapper.selectList(wrapper).stream()
                .map(TaskListItemResponse::from)
                .toList();
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        CurrentUser currentUser = CurrentUserContext.get();
        requireMentor(currentUser);
        validateUserExists(request.getAssigneeId(), "Assignee does not exist");

        LocalDateTime now = LocalDateTime.now();
        Task task = new Task();
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus() == null ? TaskStatus.TODO : request.getStatus());
        task.setPriority(request.getPriority() == null ? TaskPriority.MEDIUM : request.getPriority());
        task.setAssigneeId(request.getAssigneeId());
        task.setCreatorId(currentUser.getId());
        task.setDueDate(request.getDueDate());
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        taskMapper.insert(task);
        return TaskResponse.from(task);
    }

    public TaskResponse getTask(Long id) {
        CurrentUser currentUser = CurrentUserContext.get();
        Task task = findTask(id);
        requireVisible(currentUser, task);
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        CurrentUser currentUser = CurrentUserContext.get();
        Task task = findTask(id);
        requireEditable(currentUser, task);
        validateUserExists(request.getAssigneeId(), "Assignee does not exist");

        if (!isMentor(currentUser) && !currentUser.getId().equals(request.getAssigneeId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setAssigneeId(request.getAssigneeId());
        task.setDueDate(request.getDueDate());
        task.setUpdatedAt(LocalDateTime.now());

        taskMapper.updateById(task);
        return TaskResponse.from(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        CurrentUser currentUser = CurrentUserContext.get();
        requireMentor(currentUser);
        findTask(id);
        taskMapper.deleteById(id);
    }

    private Task findTask(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return task;
    }

    private void validateUserExists(Long userId, String message) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
    }

    private void requireVisible(CurrentUser currentUser, Task task) {
        if (isMentor(currentUser) || currentUser.getId().equals(task.getAssigneeId())) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private void requireEditable(CurrentUser currentUser, Task task) {
        requireVisible(currentUser, task);
    }

    private void requireMentor(CurrentUser currentUser) {
        if (!isMentor(currentUser)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private boolean isMentor(CurrentUser currentUser) {
        return UserRole.MENTOR.name().equals(currentUser.getRole());
    }
}
