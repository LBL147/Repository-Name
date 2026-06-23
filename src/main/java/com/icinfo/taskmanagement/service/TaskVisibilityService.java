package com.icinfo.taskmanagement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icinfo.taskmanagement.entity.Task;
import com.icinfo.taskmanagement.entity.UserRole;
import com.icinfo.taskmanagement.security.CurrentUser;
import org.springframework.stereotype.Service;

@Service
public class TaskVisibilityService {

    public LambdaQueryWrapper<Task> buildVisibleTaskQuery(CurrentUser currentUser) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        if (!isMentor(currentUser)) {
            wrapper.eq(Task::getAssigneeId, currentUser.getId());
        }
        return wrapper;
    }

    public boolean isMentor(CurrentUser currentUser) {
        return UserRole.MENTOR.name().equals(currentUser.getRole());
    }
}
