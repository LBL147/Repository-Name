import { http, unwrap } from './http';
import type { User } from '@/types/auth';

export function fetchInterns() {
  return unwrap<User[]>(http.get('/users/interns'));
}
