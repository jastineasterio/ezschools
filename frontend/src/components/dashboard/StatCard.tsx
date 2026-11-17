import type { LucideIcon } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Card } from '@/components/ui/Card';

export type StatTone = 'default' | 'success' | 'warning' | 'destructive' | 'info';

interface StatCardProps {
  label: string;
  value: string | number;
  hint?: string;
  icon?: LucideIcon;
  tone?: StatTone;
}

const toneClasses: Record<StatTone, string> = {
  default: 'text-foreground',
  success: 'text-emerald-600 dark:text-emerald-400',
  warning: 'text-amber-600 dark:text-amber-400',
  destructive: 'text-destructive',
  info: 'text-primary',
};

const toneBgClasses: Record<StatTone, string> = {
  default: 'bg-primary/10',
  success: 'bg-emerald-500/10',
  warning: 'bg-amber-500/10',
  destructive: 'bg-destructive/10',
  info: 'bg-primary/10',
};

export function StatCard({
  label,
  value,
  hint,
  icon: Icon,
  tone = 'default',
}: StatCardProps) {
  return (
    <Card className="stat-card">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="stat-card-label">{label}</p>
          <p className={cn('mt-1 text-2xl font-bold tabular-nums', toneClasses[tone])}>
            {value}
          </p>
          {hint ? <p className="stat-card-hint">{hint}</p> : null}
        </div>
        {Icon ? (
          <span className={cn('stat-card-icon', toneBgClasses[tone])}>
            <Icon className="h-5 w-5" aria-hidden="true" />
          </span>
        ) : null}
      </div>
    </Card>
  );
}
