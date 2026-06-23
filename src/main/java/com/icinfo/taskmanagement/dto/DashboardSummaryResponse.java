package com.icinfo.taskmanagement.dto;

public class DashboardSummaryResponse {

    private long todoCount;

    private long inProgressCount;

    private long doneCount;

    private long totalCount;

    private double completionRate;

    public DashboardSummaryResponse(
            long todoCount,
            long inProgressCount,
            long doneCount,
            long totalCount,
            double completionRate
    ) {
        this.todoCount = todoCount;
        this.inProgressCount = inProgressCount;
        this.doneCount = doneCount;
        this.totalCount = totalCount;
        this.completionRate = completionRate;
    }

    public long getTodoCount() {
        return todoCount;
    }

    public void setTodoCount(long todoCount) {
        this.todoCount = todoCount;
    }

    public long getInProgressCount() {
        return inProgressCount;
    }

    public void setInProgressCount(long inProgressCount) {
        this.inProgressCount = inProgressCount;
    }

    public long getDoneCount() {
        return doneCount;
    }

    public void setDoneCount(long doneCount) {
        this.doneCount = doneCount;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(double completionRate) {
        this.completionRate = completionRate;
    }
}
