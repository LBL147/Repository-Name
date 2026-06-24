import { http, unwrap } from './http';
import type { DashboardStatusChart, DashboardSummary } from '@/types/dashboard';

export function fetchDashboardSummary() {
  return unwrap<DashboardSummary>(http.get('/dashboard/summary'));
}

export function fetchDashboardStatusChart() {
  return unwrap<DashboardStatusChart>(http.get('/dashboard/status-chart'));
}
