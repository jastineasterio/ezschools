import { NavLink, useLocation } from 'react-router-dom';
import { cn } from '@/lib/utils';
import {
  LogOut,
  X,
  ChevronRight,
} from 'lucide-react';

interface NavItem {
  to: string;
  icon: React.ElementType;
  label: string;
}

interface SidebarSection {
  title: string;
  items: NavItem[];
}

interface SidebarProps {
  sections?: SidebarSection[];
  items?: NavItem[];
  open?: boolean;
  onClose?: () => void;
  brandName?: string;
  brandIcon?: React.ReactNode;
  userName?: string | null;
  userRole?: string | null;
  userInitials?: string;
  primaryColor?: string;
  onLogout?: () => void;
  avatarUrl?: string | null;
}

export function Sidebar({
  sections,
  items: flatItems,
  open = false,
  onClose,
  brandName = 'EduManage',
  brandIcon,
  userName,
  userRole,
  userInitials,
  primaryColor,
  onLogout,
  avatarUrl,
}: SidebarProps) {
  const location = useLocation();

  const navSections = sections ?? (flatItems ? [{ title: '', items: flatItems }] : []);

  const isActive = (to: string) => location.pathname === to;

  const handleLogout = async () => {
    if (onLogout) {
      await onLogout();
    } else {
      // noop
    }
  };

  return (
    <>
      <aside
        className={cn(
          'fixed top-0 left-0 z-50 w-64 h-[100dvh] flex flex-col border-r border-border bg-sidebar-background transition-transform duration-300 ease-in-out lg:translate-x-0',
          open ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        <div className="flex h-16 items-center justify-between px-4 border-b border-border shrink-0">
          <div className="flex items-center gap-3 min-w-0">
            <div
              className="h-9 w-9 rounded-xl flex items-center justify-center text-sm font-bold shadow-sm"
              style={{ backgroundColor: primaryColor }}
            >
              {brandIcon ?? (userInitials?.slice(0, 2) || 'EM').toUpperCase()}
            </div>
            <span className="font-semibold text-base truncate text-foreground tracking-tight">
              {brandName}
            </span>
          </div>
          <button
            className="lg:hidden p-1.5 rounded-md hover:bg-accent transition-colors"
            onClick={onClose}
            aria-label="Close navigation"
          >
            <X className="h-5 w-5 text-muted-foreground" />
          </button>
        </div>

        <nav className="flex-1 min-h-0 p-3 space-y-1 overflow-y-auto overscroll-y-contain">
          {navSections.map((section) => (
            <div key={section.title} className="space-y-0.5">
              {section.title && (
                <p className="px-3 text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-1 mt-2">
                  {section.title}
                </p>
              )}
              <div className="space-y-0.5">
                {section.items.map((item) => {
                  const active = isActive(item.to);
                  return (
                    <NavLink
                      key={item.to}
                      to={item.to}
                      className={({ isActive: activeLink }) => {
                        const itemActive = activeLink || active;
                        return cn(
                          'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                          itemActive
                            ? 'bg-school-active-nav-background text-school-active-nav-text shadow-sm'
                            : 'text-school-sidebar-text hover:bg-school-muted hover:text-foreground'
                        );
                      }}
                      onClick={() => {
                        onClose?.();
                      }}
                    >
                      <item.icon
                        className={cn(
                          'h-4 w-4 shrink-0',
                          isActive(item.to) ? 'text-school-active-nav-text' : 'text-school-sidebar-text'
                        )}
                        aria-hidden="true"
                      />
                      <span className={isActive(item.to) ? 'text-school-active-nav-text' : 'text-school-sidebar-text'}>
                        {item.label}
                      </span>
                      <ChevronRight className="h-3 w-3 shrink-0 opacity-0 ml-auto transition-opacity" />
                    </NavLink>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>

        <div className="mt-auto border-t border-border pt-4 pb-2 px-4 shrink-0">
          {userName && (
            <div className="mb-3 flex items-center gap-3">
              {avatarUrl ? (
                <img
                  src={avatarUrl}
                  alt="Avatar"
                  className="h-8 w-8 rounded-full border border-border/50 object-cover"
                />
              ) : (
                <div
                  className="flex h-8 w-8 items-center justify-center rounded-full text-sm font-medium shadow-sm"
                  style={{ backgroundColor: primaryColor }}
                >
                  {userInitials?.slice(0, 2) || 'U'}
                </div>
              )}
              <div className="min-w-0">
                <p className="truncate text-sm font-medium text-foreground">{userName}</p>
                {userRole && (
                  <p className="truncate text-xs text-muted-foreground">{userRole}</p>
                )}
              </div>
            </div>
          )}
          <button
            onClick={handleLogout}
            className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-destructive hover:bg-destructive/5 transition-colors"
          >
            <LogOut className="h-4 w-4" aria-hidden="true" />
            Logout
          </button>
        </div>
      </aside>

      {open && (
        <div
          className="fixed inset-0 z-40 bg-foreground/10 backdrop-blur-sm lg:hidden"
          onClick={onClose}
        />
      )}
    </>
  );
}
