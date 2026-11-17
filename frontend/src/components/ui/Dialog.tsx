import { createContext, useContext, type HTMLAttributes, type ReactNode } from 'react';
import { cn } from '@/lib/utils';
import { X } from 'lucide-react';

interface DialogContextValue {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const DialogContext = createContext<DialogContextValue | null>(null);

export function useDialog() {
  const ctx = useContext(DialogContext);
  if (!ctx) throw new Error('useDialog must be used within Dialog');
  return ctx;
}

interface DialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  children: ReactNode;
}

export function Dialog({ open, onOpenChange, children }: DialogProps) {
  if (!open) return null;
  return (
    <DialogContext.Provider value={{ open, onOpenChange }}>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 md:inset-0 md:p-0">
        <div className="fixed inset-0 bg-foreground/25 backdrop-blur-sm transition-opacity" onClick={() => onOpenChange(false)} />
        <div className="relative z-50 flex max-h-[90dvh] w-full max-w-2xl flex-col overflow-hidden rounded-2xl border border-border bg-card text-card-foreground shadow-2xl">
          {children}
        </div>
      </div>
    </DialogContext.Provider>
  );
}

interface DialogContentProps extends HTMLAttributes<HTMLDivElement> {
  title?: string;
  description?: string;
}

export function DialogContent({ className, title, description, children, ...props }: DialogContentProps) {
  return (
    <div className={cn('flex min-h-0 flex-col space-y-4 p-6', className)} {...props}>
      {(title || description) && (
        <div className="shrink-0 flex flex-col gap-1">
          {title && <h2 className="text-lg font-semibold text-card-foreground">{title}</h2>}
          {description && <p className="text-sm text-muted-foreground">{description}</p>}
        </div>
      )}
      <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain pr-1">{children}</div>
    </div>
  );
}

export function DialogClose({ onClick }: { onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className="absolute right-4 top-4 rounded-md text-muted-foreground opacity-70 hover:bg-accent hover:text-accent-foreground hover:opacity-100 transition-colors"
    >
      <X className="h-4 w-4" />
    </button>
  );
}
