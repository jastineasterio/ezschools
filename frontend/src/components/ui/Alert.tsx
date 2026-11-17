import { type HTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

export interface AlertProps extends HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'destructive' | 'success' | 'warning' | 'info';
  title?: string;
  description?: string;
}

export function Alert({ className, variant = 'default', title, description, children, ...props }: AlertProps) {
  const variantConfig: Record<string, string> = {
    default: 'border-border bg-card text-card-foreground',
    destructive: 'border-destructive/50 bg-destructive/5 text-destructive',
    success: 'border-emerald-200 bg-emerald-50/90 text-emerald-800 dark:border-emerald-900/30 dark:bg-emerald-900/20 dark:text-emerald-400',
    warning: 'border-amber-200 bg-amber-50/90 text-amber-800 dark:border-amber-900/30 dark:bg-amber-900/20 dark:text-amber-400',
    info: 'border-blue-200 bg-blue-50/90 text-blue-800 dark:border-blue-900/30 dark:bg-blue-900/20 dark:text-blue-400',
  };

  return (
    <div
      role="alert"
      className={cn('relative w-full rounded-xl border p-4 text-sm', variantConfig[variant], className)}
      {...props}
    >
      {(title || description) && (
        <div className="mb-1 flex gap-3">
          {title && <h5 className="font-semibold">{title}</h5>}
          {description && <div className="text-sm">{description}</div>}
        </div>
      )}
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
