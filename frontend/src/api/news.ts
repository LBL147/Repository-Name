import { http, unwrap } from './http';
import type { PageResponse } from '@/types/api';
import type { NewsItem, RefreshNewsPayload, RefreshTaskNewsPayload } from '@/types/news';

export function fetchNews(params: { keyword?: string; page?: number; size?: number }) {
  return unwrap<PageResponse<NewsItem>>(http.get('/news', { params }));
}

export function refreshNews(payload: RefreshNewsPayload) {
  return unwrap<NewsItem[]>(http.post('/news/refresh', payload));
}

export function fetchTaskNews(taskId: number) {
  return unwrap<NewsItem[]>(http.get(`/tasks/${taskId}/news`));
}

export function refreshTaskNews(taskId: number, payload: RefreshTaskNewsPayload) {
  return unwrap<NewsItem[]>(http.post(`/tasks/${taskId}/news/refresh`, payload));
}
