import { Bell, Sun, Moon, Calendar, Clock, User, Menu } from 'lucide-react';
import { useState } from 'react';
import { useTheme } from '@/store/ThemeContext';
import { useAuth } from '@/store/AuthContext';
import { DropdownMenu } from '@/components/ui/DropdownMenu';
import { Avatar } from '@/components/ui/Avatar';

interface TopbarProps {
  onMenuClick?: () => void;
  title?: string;
  description?: string;
  showDate?: boolean;
  primaryColor?: string;
}

export function Topbar({
  onMenuClick,
  title,
  description,
  showDate = true,
  primaryColor,
}: TopbarProps) {
  const { theme, toggleTheme } = useTheme();
  const { user } = useAuth();
  const [profileOpen, setProfileOpen] = useState(false);

  const initials = user
    ? `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase() || 'U'
    : 'U';
  const name = user ? `${user.firstName} ${user.lastName}` : 'User';
  const role = user?.roles?.[0] ?? '';
  const today = new Date().toLocaleDateString(undefined, {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  return (
    <header
      className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-border bg-header-background px-4 lg:px-8 shadow-sm backdrop-blur-md supports-[backdrop-filter]:bg-header-background/80 shrink-0"
    >
      <div className="flex items-center gap-3">
        <button
          className="lg:hidden p-2 rounded-lg hover:bg-accent hover:text-accent-foreground transition-colors"
          onClick={onMenuClick}
          aria-label="Open navigation"
        >
          <Menu className="h-5 w-5 text-muted-foreground" />
        </button>
        <div className="hidden md:flex flex-col min-w-0">
          {title && (
            <h1 className="text-lg font-semibold text-foreground tracking-tight">{title}</h1>
          )}
          {description && (
            <p className="text-xs text-muted-foreground">{description}</p>
          )}
        </div>
      </div>

      <div className="flex items-center gap-2">
        {showDate && (
          <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-lg bg-secondary text-secondary-foreground text-xs font-medium border border-border/60">
            <Calendar className="h-3.5 w-3.5" aria-hidden="true" />
            <span>{today}</span>
          </div>
        )}
        <button
          onClick={toggleTheme}
          className="p-2 rounded-lg hover:bg-accent hover:text-accent-foreground transition-colors"
          aria-label={theme === 'light' ? 'Switch to dark mode' : 'Switch to light mode'}
        >
          {theme === 'light' ? <Moon className="h-5 w-5 text-muted-foreground" /> : <Sun className="h-5 w-5 text-muted-foreground" />}
        </button>
        <button
          className="relative p-2 rounded-lg hover:bg-accent hover:text-accent-foreground transition-colors"
          aria-label="Notifications"
        >
          <Bell className="h-5 w-5 text-muted-foreground" />
          <span className="absolute right-1 top-1 h-2 w-2 rounded-full bg-destructive ring-2 ring-header-background" />
        </button>
        <div className="ml-2 flex items-center gap-2">
          <DropdownMenu
            trigger={
              <button
                onClick={() => setProfileOpen(!profileOpen)}
                className="p-2 rounded-lg hover:bg-accent hover:text-accent-foreground transition-colors"
                aria-label="Profile menu"
              >
                <Avatar className="h-8 w-8" fallback={initials} />
              </button>
            }
          >
            <div className="p-2">
              <div className="flex items-center gap-3">
                <div className="flex h-8 w-8 items-center justify-center rounded-full text-sm font-medium shadow-sm"
                  style={{ backgroundColor: primaryColor }}
                >
                  {initials}
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-medium text-foreground truncate">{name}</p>
                  <p className="text-xs text-muted-foreground truncate">{role}</p>
                </div>
              </div>
              <div className="h-px bg-border my-2" />
              <div className="flex items-center gap-2 px-3 py-2 text-sm text-foreground hover:bg-accent rounded-lg cursor-pointer" onClick={() => { setProfileOpen(false); }}
                role="menuitem">
                <User className="h-4 w-4 shrink-0 text-muted-foreground" /> My Profile
              </div>
              <div className="flex items-center gap-2 px-3 py-2 text-sm text-foreground hover:bg-accent rounded-lg cursor-pointer" onClick={() => { setProfileOpen(false); }}
                role="menuitem">
                <Clock className="h-4 w-4 shrink-0 text-muted-foreground" /> Sign out
              </div>
            </div>
          </DropdownMenu>
        </div>
      </div>
    </header>
  );
}
