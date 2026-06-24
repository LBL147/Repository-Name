import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { fetchCurrentUser, login, mockLogin, register } from '@/api/auth';
import type { AuthResponse, LoginRequest, MockLoginRequest, RegisterRequest, User } from '@/types/auth';

const TOKEN_KEY = 'tm_token';
const USER_KEY = 'tm_user';

function readStoredUser(): User | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as User;
  } catch {
    localStorage.removeItem(USER_KEY);
    return null;
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY));
  const user = ref<User | null>(readStoredUser());
  const hasRestoredSession = ref(false);
  const isAuthenticated = computed(() => Boolean(token.value && user.value));
  const isMentor = computed(() => user.value?.role === 'MENTOR');

  function setSession(session: AuthResponse) {
    token.value = session.token;
    user.value = session.user;
    hasRestoredSession.value = true;
    localStorage.setItem(TOKEN_KEY, session.token);
    localStorage.setItem(USER_KEY, JSON.stringify(session.user));
  }

  function clearSession() {
    token.value = null;
    user.value = null;
    hasRestoredSession.value = true;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  async function signIn(payload: LoginRequest) {
    setSession(await login(payload));
  }

  async function signInMock(payload: MockLoginRequest) {
    setSession(await mockLogin(payload));
  }

  async function signUp(payload: RegisterRequest) {
    setSession(await register(payload));
  }

  async function restore() {
    if (!token.value) {
      hasRestoredSession.value = true;
      return;
    }
    try {
      user.value = await fetchCurrentUser();
      localStorage.setItem(USER_KEY, JSON.stringify(user.value));
    } catch (error) {
      clearSession();
      throw error;
    } finally {
      hasRestoredSession.value = true;
    }
  }

  window.addEventListener('tm:unauthorized', clearSession);

  return {
    token,
    user,
    hasRestoredSession,
    isAuthenticated,
    isMentor,
    setSession,
    clearSession,
    signIn,
    signInMock,
    signUp,
    restore,
  };
});
