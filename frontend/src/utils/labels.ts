import type { TaskPriority, TaskStatus, UserRole } from '@/types/api';

export const roleLabels: Record<UserRole, string> = {
  MENTOR: '导师',
  INTERN: '实习生',
};

export const statusLabels: Record<TaskStatus, string> = {
  TODO: '待办',
  IN_PROGRESS: '进行中',
  DONE: '已完成',
};

export const priorityLabels: Record<TaskPriority, string> = {
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低',
};

export const statusTagTypes: Record<TaskStatus, 'info' | 'warning' | 'success'> = {
  TODO: 'info',
  IN_PROGRESS: 'warning',
  DONE: 'success',
};

export const priorityTagTypes: Record<TaskPriority, 'danger' | 'warning' | 'info'> = {
  HIGH: 'danger',
  MEDIUM: 'warning',
  LOW: 'info',
};
