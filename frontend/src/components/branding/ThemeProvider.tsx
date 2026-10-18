import { useEffect } from 'react';
import { useBranding } from '@/store/BrandingContext';

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const { branding, applyTheme } = useBranding();

  useEffect(() => {
    if (branding) {
      applyTheme(branding);
    }
  }, [branding, applyTheme]);

  useEffect(() => {
    const root = document.documentElement;
    const currentPrimary = root.style.getPropertyValue('--primary');
    const isThemeApplied = currentPrimary && !currentPrimary.includes('221.2');
    if (!isThemeApplied) {
      root.style.setProperty('--primary-foreground', '210 40% 98%');
      root.style.setProperty('--secondary-foreground', '222.2 47.4% 11.2%');
      root.style.setProperty('--muted-foreground', '215.4 16.3% 46.9%');
      root.style.setProperty('--accent-foreground', '222.2 47.4% 11.2%');
      root.style.setProperty('--destructive-foreground', '210 40% 98%');
    }
  }, []);

  return <>{children}</>;
}
