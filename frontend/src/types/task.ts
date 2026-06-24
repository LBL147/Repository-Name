import type { PageResponse, TaskPriority, TaskStatus } from './api';

export interface TaskListItemResponse {
  id: number;
  title: string;
  status: TaskStatus;
  priority: TaskPriority;
  assigneeId: number;
  creatorId: number;
  dueDate?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TaskResponse extends TaskListItemResponse {
  description?: string;
}

export interface TaskQuery {
  status?: TaskStatus;
  assigneeId?: number;
  dueDateStart?: string;
  dueDateEnd?: string;
  keyword?: string;
  page?: number;
  size?: number;
}

export interface CreateTaskPayload {
  title: string;
  description?: string;
  status?: TaskStatus;
  priority?: TaskPriority;
  assigneeId: number;
  dueDate?: string;
}

export interface UpdateTaskPayload {
  title: string;
  description?: string;
  status: TaskStatus;
  priority: TaskPriority;
  assigneeId: number;
  dueDate?: string;
}

export interface UpdateTaskStatusPayload {
  status: TaskStatus;
}

export type TaskPage = PageResponse<TaskListItemResponse>;
