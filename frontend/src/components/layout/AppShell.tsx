import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { useAuth } from '@/store/AuthContext';
import { useBranding } from '@/store/BrandingContext';
import { Sidebar } from '@/components/layout/Sidebar';
import { Topbar } from '@/components/layout/Topbar';
import type { ShellConfig } from '@/components/layout/shellConfigs';

interface AppShellProps {
  config: ShellConfig;
}

export function AppShell({ config }: AppShellProps) {
  const { user, logout } = useAuth();
  const { branding } = useBranding();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  // Close the mobile drawer on route change so it never persists across pages.
  useEffect(() => {
    if (sidebarOpen) {
      setSidebarOpen(false);
    }
  }, [location.key, sidebarOpen]);

  const primaryColor = branding?.primaryColor || '#3b82f6';
  const initials = user
    ? `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase() || 'EM'
    : 'EM';
  const name = user ? `${user.firstName} ${user.lastName}` : 'User';

  const brandIcon = branding?.logoUrl ? (
    <img src={branding.logoUrl} alt="Logo" className="h-9 w-9 rounded-xl object-contain" />
  ) : null;

  return (
    <div className="relative min-h-0 h-[100dvh] overflow-hidden bg-background">
      <Sidebar
        open={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        sections={config.sections}
        items={config.items}
        brandName={user?.school?.name ?? config.brandName}
        brandIcon={brandIcon}
        userName={name}
        userRole={config.userRole}
        userInitials={initials}
        primaryColor={primaryColor}
        onLogout={handleLogout}
      />

      <div className="flex flex-col min-w-0 min-h-0 h-[100dvh] overflow-hidden lg:ml-64">
        <Topbar
          onMenuClick={() => setSidebarOpen((prev) => !prev)}
          title={config.headerTitle}
          description={user?.school?.name ?? config.headerDescription}
          primaryColor={primaryColor}
        />
        <main className="flex-1 min-h-0 overflow-y-auto overscroll-y-contain p-4 lg:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
