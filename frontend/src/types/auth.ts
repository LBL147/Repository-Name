import type { UserRole } from './api';

export interface User {
  id: number;
  username: string;
  displayName: string;
  role: UserRole;
  createdAt?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface MockLoginRequest {
  role?: UserRole;
  username?: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  displayName: string;
  role: UserRole;
}

export interface AuthResponse {
  token: string;
  user: User;
}
