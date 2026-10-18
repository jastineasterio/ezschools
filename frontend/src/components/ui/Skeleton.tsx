import { cn } from '@/lib/utils';
import { Loader2 } from 'lucide-react';

interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {}

export function Skeleton({ className, ...props }: SkeletonProps) {
  return <div className={cn('animate-pulse rounded-md bg-gray-200', className)} {...props} />;
}

export function LoadingSpinner({ className }: { className?: string }) {
  return <Loader2 className={cn('h-6 w-6 animate-spin text-muted-foreground', className)} />;
}

