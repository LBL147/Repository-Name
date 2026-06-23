package com.icinfo.taskmanagement.dto;

import com.icinfo.taskmanagement.entity.TaskStatus;
import java.util.List;

public class DashboardStatusChartResponse {

    private List<String> legendData;

    private List<StatusChartItem> seriesData;

    public DashboardStatusChartResponse(List<String> legendData, List<StatusChartItem> seriesData) {
        this.legendData = legendData;
        this.seriesData = seriesData;
    }

    public List<String> getLegendData() {
        return legendData;
    }

    public void setLegendData(List<String> legendData) {
        this.legendData = legendData;
    }

    public List<StatusChartItem> getSeriesData() {
        return seriesData;
    }

    public void setSeriesData(List<StatusChartItem> seriesData) {
        this.seriesData = seriesData;
    }

    public static class StatusChartItem {

        private String name;

        private TaskStatus status;

        private long value;

        public StatusChartItem(String name, TaskStatus status, long value) {
            this.name = name;
            this.status = status;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public TaskStatus getStatus() {
            return status;
        }

        public void setStatus(TaskStatus status) {
            this.status = status;
        }

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }
    }
}
