import { Button } from '@/components/ui/Button';
import { useAuth } from '@/store/AuthContext';
import {
  getRoleDisplayName,
  type DashboardRole,
} from '@/lib/roles';
import { useNavigate } from 'react-router-dom';

interface RoleDashboardPageProps {
  role?: DashboardRole;
}

export function RoleDashboardPage({ role }: RoleDashboardPageProps) {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-background px-4 text-center">
      <h1 className="text-2xl font-bold text-foreground">
        {role ? `${getRoleDisplayName(role)} Dashboard` : 'Dashboard unavailable'}
      </h1>
      <p className="mt-3 max-w-lg text-muted-foreground">
        {role
          ? 'You are signed in successfully. A dedicated dashboard for this role is not available yet.'
          : 'Your account is signed in, but no dashboard is configured for its role. Please contact your school administrator.'}
      </p>
      <Button className="mt-6" onClick={handleLogout}>
        Sign out
      </Button>
    </main>
  );
}
