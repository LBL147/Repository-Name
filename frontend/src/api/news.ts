import { http, unwrap } from './http';
import type { PageResponse } from '@/types/api';
import type {
  NewsItemResponse,
  NewsQuery,
  RefreshNewsPayload,
  RefreshNewsResponse,
  RefreshTaskNewsPayload,
  RefreshTaskNewsResponse,
  TaskNewsResponse,
} from '@/types/news';

export function fetchNews(params: NewsQuery = {}) {
  return unwrap<PageResponse<NewsItemResponse>>(http.get('/news', { params }));
}

export function refreshNews(payload: RefreshNewsPayload) {
  return unwrap<RefreshNewsResponse>(http.post('/news/refresh', payload));
}

export function fetchTaskNews(taskId: number) {
  return unwrap<TaskNewsResponse[]>(http.get(`/tasks/${taskId}/news`));
}

export function refreshTaskNews(taskId: number, payload: RefreshTaskNewsPayload) {
  return unwrap<RefreshTaskNewsResponse>(http.post(`/tasks/${taskId}/news/refresh`, payload));
}
