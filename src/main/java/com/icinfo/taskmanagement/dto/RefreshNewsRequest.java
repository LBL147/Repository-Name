package com.icinfo.taskmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RefreshNewsRequest {

    @NotBlank(message = "请输入关键词")
    @Size(max = 128, message = "关键词不能超过 128 个字符")
    private String keyword;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
