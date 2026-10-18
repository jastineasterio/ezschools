import { cn } from '@/lib/utils';
import { cva, type VariantProps } from 'class-variance-authority';

const avatarVariants = cva(
  'relative flex h-10 w-10 shrink-0 overflow-hidden rounded-full',
  {
    variants: {
      size: {
        default: 'h-10 w-10',
        sm: 'h-8 w-8',
        lg: 'h-12 w-12',
        xl: 'h-16 w-16',
      },
    },
    defaultVariants: {
      size: 'default',
    },
  }
);

interface AvatarProps extends React.HTMLAttributes<HTMLDivElement>, VariantProps<typeof avatarVariants> {
  src?: string;
  alt?: string;
  fallback?: string;
}

export function Avatar({ className, size, src, alt, fallback, ...props }: AvatarProps) {
  return (
    <div className={cn(avatarVariants({ size, className }))} {...props}>
      {src ? (
        <img src={src} alt={alt || 'Avatar'} className="h-full w-full object-cover" />
      ) : (
        <div className="flex h-full w-full items-center justify-center bg-muted text-muted-foreground text-sm font-medium">
          {fallback || '?'}
        </div>
      )}
    </div>
  );
}
