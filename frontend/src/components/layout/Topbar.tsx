import { Menu, Bell, Search, Sun, Moon } from 'lucide-react';
import { useAuth } from '@/store/AuthContext';
import { useState } from 'react';
import { useTheme } from '@/store/ThemeContext';

interface TopbarProps {
  onMenuClick?: () => void;
  title?: string;
}

export function Topbar({ onMenuClick, title }: TopbarProps) {
  const { user } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const [searchOpen, setSearchOpen] = useState(false);

  return (
    <header className="h-16 border-b border-border bg-card flex items-center justify-between px-4 md:px-6">
      <div className="flex items-center gap-4">
        <button className="md:hidden" onClick={onMenuClick}>
          <Menu className="h-5 w-5" />
        </button>
        {title && <h1 className="text-lg font-semibold hidden md:block">{title}</h1>}
      </div>
      <div className="flex items-center gap-2">
        <button
          onClick={() => setSearchOpen(!searchOpen)}
          className="p-2 hover:bg-accent rounded-md"
        >
          <Search className="h-5 w-5 text-muted-foreground" />
        </button>
        <button className="p-2 hover:bg-accent rounded-md relative">
          <Bell className="h-5 w-5 text-muted-foreground" />
          <span className="absolute top-1.5 right-1.5 h-2 w-2 bg-destructive rounded-full" />
        </button>
        <button
          onClick={toggleTheme}
          className="p-2 hover:bg-accent rounded-md"
          aria-label={theme === 'light' ? 'Switch to dark mode' : 'Switch to light mode'}
        >
          {theme === 'light' ? <Moon className="h-5 w-5 text-muted-foreground" /> : <Sun className="h-5 w-5 text-muted-foreground" />}
        </button>
        <div className="flex items-center gap-2 ml-2">
          <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center text-sm font-medium text-primary">
            {user?.firstName?.[0]}{user?.lastName?.[0]}
          </div>
        </div>
      </div>
    </header>
  );
}
