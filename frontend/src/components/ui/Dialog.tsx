import { createContext, useContext } from 'react';
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
  children: React.ReactNode;
}

export function Dialog({ open, onOpenChange, children }: DialogProps) {
  if (!open) return null;
  return (
    <DialogContext.Provider value={{ open, onOpenChange }}>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div className="fixed inset-0 bg-black/50" onClick={() => onOpenChange(false)} />
        <div className="relative z-50 w-full max-w-2xl rounded-lg bg-white shadow-lg flex flex-col max-h-[90vh]">
          {children}
        </div>
      </div>
    </DialogContext.Provider>
  );
}

interface DialogContentProps extends React.HTMLAttributes<HTMLDivElement> {
  title?: string;
  description?: string;
}

export function DialogContent({ className, title, description, children, ...props }: DialogContentProps) {
  return (
    <div className={cn('flex flex-col p-6 space-y-4', className)} {...props}>
      {(title || description) && (
        <div className="flex-0">
          {title && <h2 className="text-lg font-semibold">{title}</h2>}
          {description && <p className="text-sm text-muted-foreground">{description}</p>}
        </div>
      )}
      <div className="flex-1 overflow-y-auto pr-1">{children}</div>
    </div>
  );
}

export function DialogClose({ onClick }: { onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className="absolute right-4 top-4 rounded-sm opacity-70 hover:opacity-100"
    >
      ✕
    </button>
  );
}
