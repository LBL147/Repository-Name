package com.icinfo.taskmanagement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icinfo.taskmanagement.common.ErrorCode;
import com.icinfo.taskmanagement.dto.AuthResponse;
import com.icinfo.taskmanagement.dto.LoginRequest;
import com.icinfo.taskmanagement.dto.MockLoginRequest;
import com.icinfo.taskmanagement.dto.RegisterRequest;
import com.icinfo.taskmanagement.dto.UserResponse;
import com.icinfo.taskmanagement.entity.User;
import com.icinfo.taskmanagement.entity.UserRole;
import com.icinfo.taskmanagement.exception.BusinessException;
import com.icinfo.taskmanagement.mapper.UserMapper;
import com.icinfo.taskmanagement.security.CurrentUser;
import com.icinfo.taskmanagement.security.JwtTokenProvider;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private static final String MENTOR_MOCK_USERNAME = "mentor_mock";

    private static final String INTERN_MOCK_USERNAME = "intern_mock";

    private final UserMapper userMapper;

    private final PasswordHasher passwordHasher;

    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserMapper userMapper, PasswordHasher passwordHasher, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordHasher = passwordHasher;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse login(LoginRequest request) {
        User user = findByUsername(request.getUsername());
        if (user == null || !passwordHasher.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid username or password");
        }
        return toAuthResponse(user);
    }

    public AuthResponse mockLogin(MockLoginRequest request) {
        User user = null;
        if (request != null && StringUtils.hasText(request.getUsername())) {
            user = findByUsername(request.getUsername());
        } else if (request != null && request.getRole() != null) {
            user = findMockUserByRole(request.getRole());
        }

        if (user == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "username or role is required");
        }
        return toAuthResponse(user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (findByUsername(request.getUsername()) != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordHasher.hash(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        user.setRole(request.getRole());
        user.setCreatedAt(LocalDateTime.now());

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Username already exists");
        }

        return toAuthResponse(user);
    }

    public UserResponse getCurrentUser(CurrentUser currentUser) {
        User user = userMapper.selectById(currentUser.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Current user no longer exists");
        }
        return UserResponse.from(user);
    }

    private User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
    }

    private User findMockUserByRole(UserRole role) {
        String username = role == UserRole.MENTOR ? MENTOR_MOCK_USERNAME : INTERN_MOCK_USERNAME;
        User user = findByUsername(username);
        if (user != null) {
            return user;
        }
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getRole, role)
                .last("LIMIT 1"));
    }

    private AuthResponse toAuthResponse(User user) {
        CurrentUser currentUser = new CurrentUser(user.getId(), user.getUsername(), user.getRole().name());
        String token = jwtTokenProvider.createToken(currentUser);
        return new AuthResponse(token, UserResponse.from(user));
    }
}
