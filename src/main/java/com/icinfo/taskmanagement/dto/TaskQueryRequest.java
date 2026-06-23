package com.icinfo.taskmanagement.dto;

import com.icinfo.taskmanagement.entity.TaskStatus;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class TaskQueryRequest {

    private TaskStatus status;

    private Long assigneeId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateStart;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateEnd;

    private String keyword;

    private Long page = 1L;

    private Long size = 10L;

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public LocalDate getDueDateStart() {
        return dueDateStart;
    }

    public void setDueDateStart(LocalDate dueDateStart) {
        this.dueDateStart = dueDateStart;
    }

    public LocalDate getDueDateEnd() {
        return dueDateEnd;
    }

    public void setDueDateEnd(LocalDate dueDateEnd) {
        this.dueDateEnd = dueDateEnd;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
