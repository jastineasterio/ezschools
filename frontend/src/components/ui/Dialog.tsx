import { createContext, useContext, type HTMLAttributes, type ReactNode } from 'react';
import { cn } from '@/lib/utils';

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
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div className="fixed inset-0 bg-black/50" onClick={() => onOpenChange(false)} />
        <div className="relative z-50 flex max-h-[90dvh] w-full max-w-2xl flex-col overflow-hidden rounded-lg border border-border bg-card text-card-foreground shadow-lg">
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
        <div className="shrink-0">
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
      className="absolute right-4 top-4 rounded-sm text-muted-foreground opacity-70 hover:bg-accent hover:text-accent-foreground hover:opacity-100"
    >
      ✕
    </button>
  );
}
