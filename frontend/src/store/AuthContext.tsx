import { createContext, useContext } from 'react';
import type { Dispatch, SetStateAction } from 'react';
import type { CurrentUserResponse } from '@/types/api';

export interface AuthContextValue {
  user: CurrentUserResponse | null;
  isLoading: boolean;
  sessionError: string | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<CurrentUserResponse>; // eslint-disable-line no-unused-vars
  logout: () => Promise<void>;
  setUser: Dispatch<SetStateAction<CurrentUserResponse | null>>;
  retrySession: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);

  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }

  return ctx;
}
