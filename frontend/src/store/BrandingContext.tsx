import { createContext, useContext } from 'react';
import type { SchoolBrandingResponse } from '@/types/api';
import type { SchoolColors } from '@/lib/theme';

export interface BrandingContextValue {
  branding: SchoolBrandingResponse | null;
  isLoading: boolean;
  setBranding: (branding: SchoolBrandingResponse) => void;
  /** Apply saved branding tokens to the document root (global school theme). */
  applyTheme: (branding: SchoolBrandingResponse) => void;
  /** Current school colors, falling back to defaults when none is saved. */
  colors: SchoolColors;
}

export const BrandingContext = createContext<BrandingContextValue | null>(null);

export function useBranding(): BrandingContextValue {
  const ctx = useContext(BrandingContext);
  if (!ctx) throw new Error('useBranding must be used within BrandingProvider');
  return ctx;
}
