import axios, { AxiosError } from 'axios';
import type { ApiResponse } from '@/types/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api';

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
});

function notifyUnauthorized() {
  localStorage.removeItem('tm_token');
  localStorage.removeItem('tm_user');
  window.dispatchEvent(new Event('tm:unauthorized'));
}

function isUnauthorizedPayload(data: unknown) {
  if (typeof data !== 'object' || data === null || !('code' in data)) {
    return false;
  }

  return (data as { code?: unknown }).code === 401;
}

const legacyMessageMap: Record<string, string> = {
  'Assignee does not exist': '负责人不存在',
  'Invalid username or password': '用户名或密码错误',
  'username or role is required': '请选择登录身份或输入用户名',
  'Username already exists': '用户名已存在',
  'Current user no longer exists': '当前用户不存在，请重新登录',
  'Invalid or expired token': '登录状态已失效，请重新登录',
  'Invalid request body': '请求体格式不正确',
  'keyword must not be blank': '关键词不能为空',
  'task title must not be blank': '任务标题不能为空',
  'Bad request': '请求参数错误',
  Unauthorized: '登录状态已失效，请重新登录',
  Forbidden: '没有权限执行此操作',
  'Resource not found': '资源不存在',
  'Business error': '操作失败',
  'Internal server error': '服务异常，请稍后重试',
};

function localizeErrorMessage(message?: string) {
  if (!message) {
    return '请求失败';
  }
  const statusMatch = message.match(/^Request failed with status code (\d+)$/);
  if (statusMatch) {
    const statusCode = Number(statusMatch[1]);
    if (statusCode === 401) {
      return '登录状态已失效，请重新登录';
    }
    if (statusCode === 403) {
      return '没有权限执行此操作';
    }
    if (statusCode === 404) {
      return '资源不存在';
    }
    if (statusCode >= 500) {
      return '服务异常，请稍后重试';
    }
    return '请求失败，请检查后重试';
  }
  if (message === 'Network Error') {
    return '网络连接异常，请稍后重试';
  }
  if (message.toLowerCase().includes('timeout')) {
    return '请求超时，请稍后重试';
  }
  return legacyMessageMap[message] || message;
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('tm_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => {
    if (isUnauthorizedPayload(response.data)) {
      notifyUnauthorized();
    }
    return response;
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    if (error.response?.status === 401 || error.response?.data?.code === 401) {
      notifyUnauthorized();
    }
    return Promise.reject(error);
  },
);

export async function unwrap<T>(request: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  try {
    const response = await request;
    if (response.data.code !== 0) {
      if (response.data.code === 401) {
        notifyUnauthorized();
      }
      throw new Error(localizeErrorMessage(response.data.message));
    }
    return response.data.data;
  } catch (error) {
    if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
      throw new Error(localizeErrorMessage(error.response?.data?.message || error.message));
    }
    throw error;
  }
}
