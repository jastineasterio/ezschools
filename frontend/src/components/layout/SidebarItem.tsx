import { NavLink } from 'react-router-dom';
import { cn } from '@/lib/utils';

interface SidebarItemProps {
  to: string;
  icon: React.ElementType;
  label: string;
}

export function SidebarItem({ to, icon: Icon, label }: SidebarItemProps) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        cn(
          'flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors',
          isActive
            ? 'bg-primary text-primary-foreground'
            : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'
        )
      }
    >
      <Icon className="h-4 w-4" />
      {label}
    </NavLink>
  );
}
