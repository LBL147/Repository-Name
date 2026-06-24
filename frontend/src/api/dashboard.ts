import { http, unwrap } from './http';
import type { DashboardStatusChartResponse, DashboardSummaryResponse } from '@/types/dashboard';
import type { TaskListItemResponse } from '@/types/task';

export function fetchDashboardSummary() {
  return unwrap<DashboardSummaryResponse>(http.get('/dashboard/summary'));
}

export function fetchDashboardStatusChart() {
  return unwrap<DashboardStatusChartResponse>(http.get('/dashboard/status-chart'));
}

export function fetchDashboardUpcomingTasks() {
  return unwrap<TaskListItemResponse[]>(http.get('/dashboard/upcoming-tasks'));
}

export function fetchDashboardOverdueTasks() {
  return unwrap<TaskListItemResponse[]>(http.get('/dashboard/overdue-tasks'));
}
