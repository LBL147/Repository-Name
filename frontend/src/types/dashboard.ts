export interface DashboardSummary {
  todoCount: number;
  inProgressCount: number;
  doneCount: number;
  totalCount: number;
  completionRate: number;
}

export interface DashboardStatusChartItem {
  name: string;
  status: string;
  value: number;
}

export interface DashboardStatusChart {
  legendData: string[];
  seriesData: DashboardStatusChartItem[];
}
