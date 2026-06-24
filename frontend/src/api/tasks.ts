import { http, unwrap } from './http';
import type { TaskDetail, TaskFormPayload, TaskPage, TaskQuery, UpdateTaskStatusPayload } from '@/types/task';

export function fetchTasks(params: TaskQuery) {
  return unwrap<TaskPage>(http.get('/tasks', { params }));
}

export function fetchTask(id: number) {
  return unwrap<TaskDetail>(http.get(`/tasks/${id}`));
}

export function createTask(payload: TaskFormPayload) {
  return unwrap<TaskDetail>(http.post('/tasks', payload));
}

export function updateTask(id: number, payload: TaskFormPayload) {
  return unwrap<TaskDetail>(http.put(`/tasks/${id}`, payload));
}

export function deleteTask(id: number) {
  return unwrap<void>(http.delete(`/tasks/${id}`));
}

export function updateTaskStatus(id: number, payload: UpdateTaskStatusPayload) {
  return unwrap<TaskDetail>(http.patch(`/tasks/${id}/status`, payload));
}

export function exportTasks(params: TaskQuery) {
  return http.get('/tasks/export', {
    params,
    responseType: 'blob',
  });
}
