import { Outlet, Navigate } from 'react-router-dom';
import { useAuth } from '@/store/AuthContext';
import { Loader2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { getCurrentUser } from '@/services/api';

interface ProtectedRouteProps {
  children?: React.ReactNode;
  roles?: string[];
}

export function ProtectedRoute({
  children,
  roles,
}: ProtectedRouteProps) {
  const { user, isLoading, setUser } = useAuth();
  const [checking, setChecking] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('access_token');

    if (token && !user) {
      getCurrentUser()
        .then((currentUser) => {
          setUser(currentUser);
        })
        .catch(() => {
          localStorage.removeItem('access_token');
          localStorage.removeItem('refresh_token');
        })
        .finally(() => {
          setChecking(false);
        });
    } else {
      setChecking(false);
    }
  }, [user, setUser]);

  if (isLoading || checking) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (
    roles &&
    roles.length > 0 &&
    !roles.some((requiredRole) =>
      (user.roles ?? []).some((role) => {
        const normalizedRequired = requiredRole.trim().toUpperCase();
        const normalizedUserRole = role.trim().toUpperCase();
        return normalizedUserRole === normalizedRequired || normalizedUserRole === `ROLE_${normalizedRequired}`;
      })
    )
  ) {
    return <Navigate to="/login" replace />;
  }

  return <>{children ?? <Outlet />}</>;
}
