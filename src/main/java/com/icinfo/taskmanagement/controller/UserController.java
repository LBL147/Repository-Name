package com.icinfo.taskmanagement.controller;

import com.icinfo.taskmanagement.common.ApiResponse;
import com.icinfo.taskmanagement.dto.UserResponse;
import com.icinfo.taskmanagement.service.UserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/interns")
    public ApiResponse<List<UserResponse>> listInterns() {
        return ApiResponse.success(userService.listInterns());
    }
}
