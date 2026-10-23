import { Outlet, Navigate } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from '@/store/AuthContext';
import { Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Alert } from '@/components/ui/Alert';
import { getDashboardPath, hasRole } from '@/lib/roles';

interface ProtectedRouteProps {
  children?: ReactNode;
  roles?: string[];
}

export function ProtectedRoute({
  children,
  roles,
}: ProtectedRouteProps) {
  const { user, isLoading, sessionError, retrySession } = useAuth();

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (sessionError) {
    return (
      <div className="flex min-h-screen items-center justify-center px-4">
        <div className="w-full max-w-md space-y-4">
          <Alert variant="destructive" message={sessionError} />
          <Button onClick={() => void retrySession()}>Retry</Button>
        </div>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (
    roles &&
    roles.length > 0 &&
    !roles.some((requiredRole) => hasRole(user.roles, requiredRole))
  ) {
    return <Navigate to={getDashboardPath(user.roles)} replace />;
  }

  return <>{children ?? <Outlet />}</>;
}
