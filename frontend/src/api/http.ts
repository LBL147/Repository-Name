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
      throw new Error(response.data.message || '请求失败');
    }
    return response.data.data;
  } catch (error) {
    if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
      throw new Error(error.response?.data?.message || error.message || '请求失败');
    }
    throw error;
  }
}
