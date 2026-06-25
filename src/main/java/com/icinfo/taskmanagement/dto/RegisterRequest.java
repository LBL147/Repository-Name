package com.icinfo.taskmanagement.dto;

import com.icinfo.taskmanagement.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "请输入用户名")
    @Size(max = 64, message = "用户名不能超过 64 个字符")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 128, message = "密码长度需为 6 到 128 个字符")
    private String password;

    @NotBlank(message = "请输入姓名")
    @Size(max = 64, message = "姓名不能超过 64 个字符")
    private String displayName;

    @NotNull(message = "请选择角色")
    private UserRole role;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
