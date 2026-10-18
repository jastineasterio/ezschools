import { useState, useEffect } from 'react';
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
  children: React.ReactNode;
}) {
  const [user, setUser] = useState<CurrentUserResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = getAccessToken();

    if (!token) {
      setIsLoading(false);
      return;
    }

    getCurrentUser()
      .then((currentUser) => {
        setUser(currentUser);
      })
      .catch(() => {
        clearTokens();
        setUser(null);
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, []);

  const login = async (
    email: string,
    password: string
  ): Promise<CurrentUserResponse> => {
    await apiLogin(email, password);

    const currentUser = await getCurrentUser();

    setUser(currentUser);

    return currentUser;
  };

  const logout = async () => {
    try {
      await apiLogout();
    } catch {
      // Ignore logout API errors.
    } finally {
      clearTokens();
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isLoading,
        isAuthenticated: !!user,
        login,
        logout,
        setUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
