package com.icinfo.taskmanagement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icinfo.taskmanagement.dto.UserResponse;
import com.icinfo.taskmanagement.entity.User;
import com.icinfo.taskmanagement.entity.UserRole;
import com.icinfo.taskmanagement.mapper.UserMapper;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public List<UserResponse> listInterns() {
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .eq(User::getRole, UserRole.INTERN))
                .stream()
                .sorted(Comparator
                        .comparing(this::displayNameForSort, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(User::getId))
                .map(UserResponse::from)
                .toList();
    }

    private String displayNameForSort(User user) {
        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        return user.getUsername();
    }
}
