import type { PageResponse, TaskPriority, TaskStatus } from './api';

export interface TaskListItem {
  id: number;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: TaskPriority;
  assigneeId: number;
  assigneeName?: string;
  creatorId: number;
  creatorName?: string;
  dueDate?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TaskDetail extends TaskListItem {}

export interface TaskQuery {
  status?: TaskStatus;
  assigneeId?: number;
  dueDateStart?: string;
  dueDateEnd?: string;
  keyword?: string;
  page?: number;
  size?: number;
}

export interface TaskFormPayload {
  title: string;
  description?: string;
  status?: TaskStatus;
  priority?: TaskPriority;
  assigneeId: number;
  dueDate?: string;
}

export interface UpdateTaskStatusPayload {
  status: TaskStatus;
}

export type TaskPage = PageResponse<TaskListItem>;
