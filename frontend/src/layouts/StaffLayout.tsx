import { useState } from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import {
  BookOpenCheck,
  Building2,
  Bus,
  CreditCard,
  FileText,
  Award,
  KeyRound,
  GraduationCap,
  LayoutDashboard,
  LogOut,
  Menu,
  Moon,
  Receipt,
  Sun,
  Users,
  Wallet,
  X,
} from 'lucide-react';
import { useAuth } from '@/store/AuthContext';
import { useBranding } from '@/store/BrandingContext';
import { useTheme } from '@/store/ThemeContext';
import { getRoleDisplayName, type DashboardRole } from '@/lib/roles';

interface NavItem {
  to: string;
  label: string;
  icon: typeof LayoutDashboard;
}

const navByRole: Record<DashboardRole, NavItem[]> = {
  SUPER_ADMIN: [{ to: '/platform/dashboard', label: 'Dashboard', icon: LayoutDashboard }],
  MANAGER: [{ to: '/school/dashboard', label: 'Dashboard', icon: LayoutDashboard }],
  TEACHER: [{ to: '/school/teacher/dashboard', label: 'Dashboard', icon: LayoutDashboard }],
  STUDENT: [
    { to: '/school/student/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/school/student/certificates', label: 'Certificates', icon: Award },
    { to: '/school/student/change-password', label: 'Change Password', icon: KeyRound },
  ],
  PARENT: [{ to: '/school/parent/dashboard', label: 'Dashboard', icon: LayoutDashboard }],
  ACCOUNTANT: [
    { to: '/school/accountant/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/school/accountant/payments', label: 'Payments', icon: CreditCard },
    { to: '/school/accountant/fee-assignments', label: 'Fee Assignments', icon: Receipt },
    { to: '/school/accountant/fee-structures', label: 'Fee Structures', icon: FileText },
    { to: '/school/accountant/finance', label: 'Finance', icon: Wallet },
  ],
  LIBRARIAN: [
    { to: '/school/librarian/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/school/librarian/library', label: 'Library', icon: BookOpenCheck },
  ],
  HOD: [
    { to: '/school/hod/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/school/hod/departments', label: 'Departments', icon: Users },
    { to: '/school/hod/subjects', label: 'Subjects', icon: FileText },
    { to: '/school/hod/examinations', label: 'Examinations', icon: GraduationCap },
    { to: '/school/hod/results', label: 'Results', icon: FileText },
  ],
  WARDEN: [
    { to: '/school/warden/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/school/warden/boarding', label: 'Boarding', icon: Building2 },
  ],
  TRANSPORT_OFFICER: [
    { to: '/school/transport-officer/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/school/transport-officer/transport', label: 'Transport', icon: Bus },
  ],
};

interface StaffLayoutProps {
  role: DashboardRole;
}

export function StaffLayout({ role }: StaffLayoutProps) {
  const { user, logout } = useAuth();
  const { branding } = useBranding();
  const { theme, toggleTheme } = useTheme();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const navigate = useNavigate();

  const navItems = navByRole[role] ?? [];
  const roleLabel = getRoleDisplayName(role);
  const primaryColor = branding?.primaryColor || '#3b82f6';

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="min-h-screen bg-background">
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-64 transform bg-card border-r border-border transition-transform duration-200 ease-in-out lg:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex h-16 items-center justify-between border-b border-border px-4">
          <div className="flex min-w-0 items-center gap-2">
            {branding?.logoUrl ? (
              <img
                src={branding.logoUrl}
                alt=""
                className="h-8 w-8 rounded object-contain"
              />
            ) : (
              <div
                className="flex h-8 w-8 items-center justify-center rounded-lg text-sm font-bold text-primary-foreground"
                style={{ backgroundColor: primaryColor }}
              >
                EM
              </div>
            )}
            <span className="truncate text-lg font-semibold text-foreground">
              {user?.school?.name || 'EduManage'}
            </span>
          </div>
          <button
            className="lg:hidden"
            onClick={() => setSidebarOpen(false)}
            aria-label="Close navigation"
          >
            <X className="h-5 w-5 text-muted-foreground" />
          </button>
        </div>

        <nav className="h-[calc(100vh-11rem)] space-y-1 overflow-y-auto p-4">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive: active }) =>
                `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                  active
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'
                }`
              }
            >
              <item.icon className="h-4 w-4" aria-hidden="true" />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="absolute bottom-0 w-full border-t border-border p-4">
          <div className="mb-3 flex items-center gap-3">
            <div
              className="flex h-8 w-8 items-center justify-center rounded-full text-sm font-medium text-primary-foreground"
              style={{ backgroundColor: primaryColor }}
            >
              {user?.firstName?.[0] || 'U'}
            </div>
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-foreground">
                {user?.firstName} {user?.lastName}
              </p>
              <p className="truncate text-xs text-muted-foreground">{roleLabel}</p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-destructive hover:bg-destructive/10"
          >
            <LogOut className="h-4 w-4" aria-hidden="true" />
            Logout
          </button>
        </div>
      </aside>

      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-foreground/10 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      <div className="lg:pl-64">
        <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-border bg-card px-4 lg:px-8">
          <div className="flex items-center gap-4">
            <button
              className="lg:hidden"
              onClick={() => setSidebarOpen(true)}
              aria-label="Open navigation"
            >
              <Menu className="h-5 w-5 text-muted-foreground" />
            </button>
            <h1 className="text-lg font-semibold text-foreground">{roleLabel} Workspace</h1>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={toggleTheme}
              className="rounded-md p-2 hover:bg-accent hover:text-accent-foreground"
              aria-label={
                theme === 'light' ? 'Switch to dark mode' : 'Switch to light mode'
              }
            >
              {theme === 'light' ? (
                <Moon className="h-5 w-5 text-muted-foreground" />
              ) : (
                <Sun className="h-5 w-5 text-muted-foreground" />
              )}
            </button>
            <div className="ml-2 hidden items-center gap-2 md:flex">
              <div
                className="flex h-8 w-8 items-center justify-center rounded-full text-sm font-medium text-primary-foreground"
                style={{ backgroundColor: primaryColor }}
              >
                {user?.firstName?.[0] || 'U'}
              </div>
              <div>
                <p className="text-sm font-medium text-foreground">
                  {user?.firstName} {user?.lastName}
                </p>
                <p className="text-xs text-muted-foreground">{roleLabel}</p>
              </div>
            </div>
          </div>
        </header>
        <main className="p-4 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
