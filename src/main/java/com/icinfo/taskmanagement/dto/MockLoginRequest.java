package com.icinfo.taskmanagement.dto;

import com.icinfo.taskmanagement.entity.UserRole;

public class MockLoginRequest {

    private String username;

    private UserRole role;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
