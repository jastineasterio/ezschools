import type { HTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

export function Separator({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div role="separator" className={cn('h-[1px] w-full shrink-0 bg-border', className)} {...props} />;
}
