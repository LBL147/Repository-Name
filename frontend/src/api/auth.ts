import { http, unwrap } from './http';
import type { AuthResponse, LoginRequest, MockLoginRequest, RegisterRequest, User } from '@/types/auth';

export function login(payload: LoginRequest) {
  return unwrap<AuthResponse>(http.post('/auth/login', payload));
}

export function mockLogin(payload: MockLoginRequest) {
  return unwrap<AuthResponse>(http.post('/auth/mock-login', payload));
}

export function register(payload: RegisterRequest) {
  return unwrap<AuthResponse>(http.post('/auth/register', payload));
}

export function fetchCurrentUser() {
  return unwrap<User>(http.get('/auth/me'));
}
