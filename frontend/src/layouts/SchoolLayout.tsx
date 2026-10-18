import { Outlet, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '@/store/AuthContext';
import { useBranding } from '@/store/BrandingContext';
import {
  LayoutDashboard,
  School,
  Users,
  Palette,
  LogOut,
  Menu,
  X,
  Bell,
  BookOpen,
  CalendarDays,
  ClipboardCheck,
  GraduationCap,
  FileText,
  UserCheck,
  Wallet,
  Receipt,
  CreditCard,
  Building2,
  MessageSquare,
  Bus,
  BookOpenCheck,
  Sun,
  Moon,
} from 'lucide-react';
import { useState } from 'react';
import { logout } from '@/services/api';
import { useTheme } from '@/store/ThemeContext';

const navSections = [
  {
    title: 'Main',
    items: [
      { to: '/school/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
      { to: '/school/profile', icon: School, label: 'School Profile' },
    ],
  },
  {
    title: 'Academic',
    items: [
      { to: '/school/academic-years', icon: CalendarDays, label: 'Academic Years' },
      { to: '/school/classes', icon: BookOpen, label: 'Classes' },
      { to: '/school/streams', icon: BookOpen, label: 'Streams' },
      { to: '/school/subjects', icon: BookOpen, label: 'Subjects' },
      { to: '/school/departments', icon: Users, label: 'Departments' },
    ],
  },
  {
    title: 'People',
    items: [
      { to: '/school/students', icon: Users, label: 'Students' },
      { to: '/school/teachers', icon: UserCheck, label: 'Teachers' },
      { to: '/school/parents', icon: Users, label: 'Parents/Guardians' },
    ],
  },
  {
    title: 'Boarding',
    items: [
      { to: '/school/boarding', icon: Building2, label: 'Boarding' },
    ],
  },
  {
    title: 'Assessments',
    items: [
      { to: '/school/attendance', icon: ClipboardCheck, label: 'Attendance' },
      { to: '/school/examinations', icon: GraduationCap, label: 'Examinations' },
      { to: '/school/results', icon: FileText, label: 'Results' },
    ],
  },
  {
    title: 'Finance',
    items: [
      { to: '/school/finance', icon: Wallet, label: 'Finance' },
      { to: '/school/fee-structures', icon: Receipt, label: 'Fee Structures' },
      { to: '/school/fee-assignments', icon: CreditCard, label: 'Fee Assignments' },
      { to: '/school/payments', icon: CreditCard, label: 'Payments' },
    ],
  },
  {
    title: 'Communications',
    items: [
      { to: '/school/sms', icon: MessageSquare, label: 'SMS' },
    ],
  },
  {
    title: 'Communication',
    items: [
      { to: '/school/notifications', icon: Bell, label: 'Notifications' },
      { to: '/school/reports', icon: FileText, label: 'Reports' },
    ],
  },
  {
    title: 'System',
    items: [
      { to: '/school/branding', icon: Palette, label: 'Branding' },
      { to: '/school/users', icon: Users, label: 'Users' },
    ],
  },
  {
    title: 'Transport',
    items: [
      { to: '/school/transport', icon: Bus, label: 'Transport' },
    ],
  },
  {
    title: 'Library',
    items: [
      { to: '/school/library', icon: BookOpenCheck, label: 'Library' },
    ],
  },
];
export function SchoolLayout() {
  const { user } = useAuth();
  const { branding } = useBranding();
  const { theme, toggleTheme } = useTheme();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const isActive = (to: string) => location.pathname === to;

  const handleLogout = async () => {
    await logout();
    navigate('/login');
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
            {branding?.logoUrl ? (
              <img src={branding.logoUrl} alt="Logo" className="h-8 w-8 rounded object-contain" />
            ) : (
              <div
                className="h-8 w-8 rounded-lg flex items-center justify-center text-primary-foreground font-bold text-sm"
                style={{ backgroundColor: primaryColor }}
              >
                EM
              </div>
            )}
            <span className="font-semibold text-lg truncate text-foreground">{user?.school?.name || 'School'}</span>
          </div>
          <button className="lg:hidden" onClick={() => setSidebarOpen(false)}>
            <X className="h-5 w-5 text-muted-foreground" />
          </button>
        </div>
        <nav className="p-4 space-y-4 overflow-y-auto h-[calc(100vh-8rem)]">
          {navSections.map((section) => (
            <div key={section.title}>
              <p className="px-3 text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2">
                {section.title}
              </p>
              <div className="space-y-1">
                {section.items.map((item) => (
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
              </div>
            </div>
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
            <h1 className="text-lg font-semibold text-foreground">School Management</h1>
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
                <p className="text-xs text-muted-foreground">Manager</p>
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
