import axios from 'axios';
import type { ReactNode } from 'react';
import { useState, useEffect, useCallback } from 'react';
import {
  clearTokens,
  getAccessToken,
  getCurrentUser,
  login as apiLogin,
  logout as apiLogout,
} from '@/services/api';

import type { CurrentUserResponse } from '@/types/api';
import { AuthContext } from '@/store/AuthContext';

export function AuthProvider({
  children,
}: {
  children: ReactNode;
}) {
  const [user, setUser] = useState<CurrentUserResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [sessionError, setSessionError] = useState<string | null>(null);

  const retrySession = useCallback(async () => {
    setIsLoading(true);
    setSessionError(null);

    try {
      const currentUser = await getCurrentUser();
      setUser(currentUser);
    } catch (error) {
      if (
        (axios.isAxiosError(error) && error.response?.status === 401) ||
        !getAccessToken()
      ) {
        clearTokens();
        setUser(null);
      } else {
        setSessionError('Unable to verify your session. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    const token = getAccessToken();

    if (!token) {
      setIsLoading(false);
      return;
    }

    void retrySession();
  }, [retrySession]);

  const login = async (
    email: string,
    password: string
  ): Promise<CurrentUserResponse> => {
    setIsLoading(true);
    setSessionError(null);
    try {
      await apiLogin(email, password);
      const currentUser = await getCurrentUser();
      setUser(currentUser);
      return currentUser;
    } catch (error) {
      if (
        (axios.isAxiosError(error) && error.response?.status === 401) ||
        !getAccessToken()
      ) {
        clearTokens();
        setUser(null);
      } else {
        setSessionError('Unable to verify your session. Please try again.');
      }
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    try {
      await apiLogout();
    } catch {
      // Ignore logout API errors.
    } finally {
      clearTokens();
      setUser(null);
      try {
      // Clear every school-branding cache entry so no school's logo or colors
      // persist into the next session.
        for (let i = sessionStorage.length - 1; i >= 0; i--) {
          const key = sessionStorage.key(i);
          if (key != null && key.startsWith('edumanage:school-branding:')) {
            sessionStorage.removeItem(key);
          }
        }
      } catch {
        // ignore
      }
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isLoading,
        sessionError,
        isAuthenticated: !!user,
        login,
        logout,
        setUser,
        retrySession,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
