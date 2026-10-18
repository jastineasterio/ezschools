import { useState, useRef, useEffect } from 'react';
import { cn } from '@/lib/utils';

interface DropdownMenuProps {
  trigger: React.ReactNode;
  children: React.ReactNode;
  align?: 'left' | 'right';
}

export function DropdownMenu({ trigger, children, align = 'right' }: DropdownMenuProps) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (ref.current && !ref.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className="relative" ref={ref}>
      <div onClick={() => setOpen(!open)}>{trigger}</div>
      {open && (
        <div
          className={cn(
            'absolute z-50 mt-2 min-w-[8rem] rounded-md border bg-white shadow-md',
            align === 'right' ? 'right-0' : 'left-0'
          )}
        >
          <div className="py-1">{children}</div>
        </div>
      )}
    </div>
  );
}

export function DropdownMenuItem({
  onClick,
  children,
  className,
}: {
  onClick?: () => void;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <button
      onClick={() => {
        onClick?.();
      }}
      className={cn(
        'flex w-full items-center px-4 py-2 text-sm text-foreground hover:bg-muted',
        className
      )}
    >
      {children}
    </button>
  );
}

