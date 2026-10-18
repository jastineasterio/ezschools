import { useBranding } from '@/store/BrandingContext';
import { cn } from '@/lib/utils';

interface LogoProps {
  className?: string;
}

export function Logo({ className }: LogoProps) {
  const { branding } = useBranding();

  if (branding?.logoUrl) {
    return (
      <img
        src={branding.logoUrl}
        alt="School Logo"
        className={cn('h-8 w-auto object-contain', className)}
      />
    );
  }

  return (
    <div className={cn('flex items-center gap-2', className)}>
      <div className="h-8 w-8 rounded-lg bg-primary flex items-center justify-center text-primary-foreground font-bold text-sm">
        EM
      </div>
      <span className="text-lg font-bold text-primary">EduManage</span>
    </div>
  );
}
