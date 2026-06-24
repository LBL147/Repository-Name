import type { TaskStatus } from './api';

export interface DashboardSummaryResponse {
  todoCount: number;
  inProgressCount: number;
  doneCount: number;
  totalCount: number;
  completionRate: number;
}

export interface DashboardStatusChartItem {
  name: string;
  status: TaskStatus;
  value: number;
}

export interface DashboardStatusChartResponse {
  legendData: string[];
  seriesData: DashboardStatusChartItem[];
}

export type DashboardSummary = DashboardSummaryResponse;

export type DashboardStatusChart = DashboardStatusChartResponse;
