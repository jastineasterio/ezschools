import { SidebarItem } from '@/components/layout/SidebarItem';
import {
  LayoutDashboard,
  School,
  Package,
  CreditCard,
  Users,
  MessageSquare,
  BarChart3,
  FileText,
  Settings,
  LogOut,
} from 'lucide-react';
import { useAuth } from '@/store/AuthContext';
import { useNavigate } from 'react-router-dom';

const navItems = [
  { to: '/platform/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/platform/schools', icon: School, label: 'Schools' },
  { to: '/platform/packages', icon: Package, label: 'Packages' },
  { to: '/platform/subscriptions', icon: CreditCard, label: 'Subscriptions' },
  { to: '/platform/users', icon: Users, label: 'Platform Users' },
  { to: '/platform/sms', icon: MessageSquare, label: 'SMS Configuration' },
  { to: '/platform/analytics', icon: BarChart3, label: 'Analytics' },
  { to: '/platform/audit-logs', icon: FileText, label: 'Audit Logs' },
  { to: '/platform/settings', icon: Settings, label: 'Settings' },
];

export function Sidebar() {
  const { logout, user } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <aside className="w-64 bg-card border-r border-border flex flex-col h-screen">
      <div className="h-16 flex items-center px-4 border-b border-border">
        <span className="text-lg font-bold text-primary">EduManage</span>
      </div>
      <nav className="flex-1 p-4 space-y-1 overflow-auto">
        {navItems.map((item) => (
          <SidebarItem key={item.to} to={item.to} icon={item.icon} label={item.label} />
        ))}
      </nav>
      <div className="p-4 border-t border-border">
        <div className="flex items-center gap-3 mb-3">
          <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center text-sm font-medium text-primary">
            {user?.firstName?.[0]}{user?.lastName?.[0]}
          </div>
          <div className="min-w-0">
            <p className="text-sm font-medium truncate">{user?.firstName} {user?.lastName}</p>
            <p className="text-xs text-muted-foreground truncate">{user?.email}</p>
          </div>
        </div>
        <button
          onClick={handleLogout}
          className="flex items-center gap-2 w-full px-3 py-2 text-sm text-destructive hover:bg-destructive/10 rounded-md"
        >
          <LogOut className="h-4 w-4" />
          Logout
        </button>
      </div>
    </aside>
  );
}
