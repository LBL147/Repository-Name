package com.icinfo.taskmanagement.controller;

import com.icinfo.taskmanagement.common.ApiResponse;
import com.icinfo.taskmanagement.common.ErrorCode;
import com.icinfo.taskmanagement.dto.AuthResponse;
import com.icinfo.taskmanagement.dto.LoginRequest;
import com.icinfo.taskmanagement.dto.MockLoginRequest;
import com.icinfo.taskmanagement.dto.RegisterRequest;
import com.icinfo.taskmanagement.dto.UserResponse;
import com.icinfo.taskmanagement.exception.BusinessException;
import com.icinfo.taskmanagement.security.CurrentUser;
import com.icinfo.taskmanagement.security.JwtTokenProvider;
import com.icinfo.taskmanagement.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/mock-login")
    public ApiResponse<AuthResponse> mockLogin(@RequestBody(required = false) MockLoginRequest request) {
        return ApiResponse.success(authService.mockLogin(request));
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(HttpServletRequest request) {
        CurrentUser currentUser = jwtTokenProvider.parse(resolveToken(request));
        return ApiResponse.success(authService.getCurrentUser(currentUser));
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return authorization.substring(7);
    }
}
