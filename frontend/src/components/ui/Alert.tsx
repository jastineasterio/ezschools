import { type HTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

export interface AlertProps extends HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'destructive';
  message?: string;
}

export function Alert({ className, variant = 'default', message, children, ...props }: AlertProps) {
  return (
    <div
      role="alert"
      className={cn(
        'relative w-full rounded-lg border px-4 py-3 text-sm',
        variant === 'destructive' ? 'border-destructive/50 text-destructive' : 'border-border bg-card text-card-foreground',
        className
      )}
      {...props}
    >
      {message && <p className="text-sm">{message}</p>}
      {children}
    </div>
  );
}

export function AlertTitle({ className, ...props }: HTMLAttributes<HTMLHeadingElement>) {
  return <h5 className={cn('mb-1 font-medium leading-none tracking-tight', className)} {...props} />;
}

export function AlertDescription({ className, ...props }: HTMLAttributes<HTMLParagraphElement>) {
  return <div className={cn('text-sm [&_p]:leading-relaxed', className)} {...props} />;
}
