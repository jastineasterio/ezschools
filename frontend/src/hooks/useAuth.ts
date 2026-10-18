import { useAuth } from '@/store/AuthContext';

export function useAuthStatus() {
  const { isAuthenticated, isLoading } = useAuth();
  return { isAuthenticated, isLoading };
}
