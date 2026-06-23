package com.icinfo.taskmanagement.dto;

import com.alibaba.excel.annotation.ExcelProperty;

public class TaskExportRow {

    @ExcelProperty("标题")
    private String title;

    @ExcelProperty("描述")
    private String description;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("优先级")
    private String priority;

    @ExcelProperty("负责人")
    private String assignee;

    @ExcelProperty("创建人")
    private String creator;

    @ExcelProperty("截止日期")
    private String dueDate;

    @ExcelProperty("创建时间")
    private String createdAt;

    @ExcelProperty("更新时间")
    private String updatedAt;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
