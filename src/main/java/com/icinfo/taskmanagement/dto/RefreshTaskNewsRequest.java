package com.icinfo.taskmanagement.dto;

import jakarta.validation.constraints.Size;

public class RefreshTaskNewsRequest {

    @Size(max = 128)
    private String keyword;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
