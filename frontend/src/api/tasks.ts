import { http, unwrap } from './http';
import type {
  CreateTaskPayload,
  TaskPage,
  TaskQuery,
  TaskResponse,
  UpdateTaskPayload,
  UpdateTaskStatusPayload,
} from '@/types/task';

export function fetchTasks(params: TaskQuery = {}) {
  return unwrap<TaskPage>(http.get('/tasks', { params }));
}

export function fetchTask(id: number) {
  return unwrap<TaskResponse>(http.get(`/tasks/${id}`));
}

export function createTask(payload: CreateTaskPayload) {
  return unwrap<TaskResponse>(http.post('/tasks', payload));
}

export function updateTask(id: number, payload: UpdateTaskPayload) {
  return unwrap<TaskResponse>(http.put(`/tasks/${id}`, payload));
}

export function deleteTask(id: number) {
  return unwrap<void>(http.delete(`/tasks/${id}`));
}

export function updateTaskStatus(id: number, payload: UpdateTaskStatusPayload) {
  return unwrap<TaskResponse>(http.patch(`/tasks/${id}/status`, payload));
}

export function exportTasks(params: TaskQuery) {
  return http.get('/tasks/export', {
    params,
    responseType: 'blob',
  });
}
