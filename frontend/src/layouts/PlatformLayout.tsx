import { Outlet, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '@/store/AuthContext';
import { useBranding } from '@/store/BrandingContext';
import {
  LayoutDashboard,
  School,
  Package,
  CreditCard,
  Users,
  Settings,
  FileText,
  BarChart3,
  LogOut,
  Menu,
  X,
  Bell,
  MessageSquare,
  Sun,
  Moon,
} from 'lucide-react';
import { useState } from 'react';
import { useTheme } from '@/store/ThemeContext';

const navItems = [
  { to: '/platform/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/platform/schools', icon: School, label: 'Schools' },
  { to: '/platform/packages', icon: Package, label: 'Packages' },
  { to: '/platform/subscriptions', icon: CreditCard, label: 'Subscriptions' },
  { to: '/platform/users', icon: Users, label: 'Platform Users' },
  { to: '/platform/audit-logs', icon: FileText, label: 'Audit Logs' },
  { to: '/platform/analytics', icon: BarChart3, label: 'Analytics' },
  { to: '/platform/settings', icon: Settings, label: 'Settings' },
  { to: '/platform/sms-configurations', icon: MessageSquare, label: 'SMS Configurations' },
  { to: '/platform/notifications', icon: Bell, label: 'Notification Management' },
];

export function PlatformLayout() {
  const { user, logout } = useAuth();
  const { branding } = useBranding();
  const { theme, toggleTheme } = useTheme();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const isActive = (to: string) => location.pathname === to;

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  const primaryColor = branding?.primaryColor || '#3b82f6';

  return (
    <div className="min-h-screen bg-background">
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-64 transform bg-card border-r border-border transition-transform duration-200 ease-in-out lg:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex h-16 items-center justify-between px-4 border-b border-border">
          <div className="flex items-center gap-2">
            <div
              className="h-8 w-8 rounded-lg flex items-center justify-center text-primary-foreground font-bold text-sm"
              style={{ backgroundColor: primaryColor }}
            >
              EM
            </div>
            <span className="font-semibold text-lg text-foreground">EduManage</span>
          </div>
          <button className="lg:hidden" onClick={() => setSidebarOpen(false)}>
            <X className="h-5 w-5 text-muted-foreground" />
          </button>
        </div>
        <nav className="p-4 space-y-1">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'
                }`
              }
              onClick={() => setSidebarOpen(false)}
            >
              <item.icon className={`h-4 w-4 ${isActive(item.to) ? 'text-primary-foreground' : 'text-muted-foreground'}`} />
              <span className={isActive(item.to) ? 'text-primary-foreground' : 'text-muted-foreground'}>{item.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="absolute bottom-0 w-full border-t border-border p-4">
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-destructive hover:bg-destructive/10 w-full"
          >
            <LogOut className="h-4 w-4" />
            Logout
          </button>
        </div>
      </aside>

      {sidebarOpen && (
        <div className="fixed inset-0 z-40 bg-foreground/10 lg:hidden" onClick={() => setSidebarOpen(false)} />
      )}

      <div className="lg:pl-64">
        <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-border bg-card px-4 lg:px-8">
          <div className="flex items-center gap-4">
            <button className="lg:hidden" onClick={() => setSidebarOpen(true)}>
              <Menu className="h-5 w-5 text-muted-foreground" />
            </button>
            <h1 className="text-lg font-semibold text-foreground">Platform Administration</h1>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={toggleTheme}
              className="p-2 rounded-md hover:bg-accent hover:text-accent-foreground"
              aria-label={theme === 'light' ? 'Switch to dark mode' : 'Switch to light mode'}
            >
              {theme === 'light' ? <Moon className="h-5 w-5 text-muted-foreground" /> : <Sun className="h-5 w-5 text-muted-foreground" />}
            </button>
            <button className="relative rounded-full p-2 hover:bg-accent">
              <Bell className="h-5 w-5 text-muted-foreground" />
              <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-destructive" />
            </button>
            <div className="flex items-center gap-2 ml-2">
              <div
                className="h-8 w-8 rounded-full flex items-center justify-center text-primary-foreground text-sm font-medium"
                style={{ backgroundColor: primaryColor }}
              >
                {user?.firstName?.[0] || 'U'}
              </div>
              <div className="hidden md:block">
                <p className="text-sm font-medium text-foreground">
                  {user?.firstName} {user?.lastName}
                </p>
                <p className="text-xs text-muted-foreground">Super Admin</p>
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
