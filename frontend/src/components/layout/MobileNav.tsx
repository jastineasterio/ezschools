import { NavLink } from 'react-router-dom';
import { cn } from '@/lib/utils';

interface MobileNavProps {
  items: { to: string; icon: React.ElementType; label: string }[];
}

export function MobileNav({ items }: MobileNavProps) {
  return (
    <nav className="md:hidden fixed bottom-0 left-0 right-0 bg-card border-t border-border z-50">
      <div className="flex items-center justify-around py-2">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              cn(
                'flex flex-col items-center gap-1 p-2 text-xs',
                isActive ? 'text-primary' : 'text-muted-foreground'
              )
            }
          >
            <item.icon className="h-5 w-5" />
            {item.label}
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
