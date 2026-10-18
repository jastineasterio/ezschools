import { createContext, useContext } from 'react';
import type { CurrentUserResponse } from '@/types/api';

export interface AuthContextValue {
  user: CurrentUserResponse | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (
    email: string,
    password: string
  ) => Promise<CurrentUserResponse>;
  logout: () => Promise<void>;
  setUser: (user: CurrentUserResponse | null) => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);

  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }

  return ctx;
}
