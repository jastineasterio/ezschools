import { createContext, useContext } from 'react';
import type { SchoolBrandingResponse } from '@/types/api';

export interface BrandingContextValue {
  branding: SchoolBrandingResponse | null;
  isLoading: boolean;
  setBranding: (branding: SchoolBrandingResponse) => void;
  applyTheme: (branding: SchoolBrandingResponse) => void;
}

export const BrandingContext = createContext<BrandingContextValue | null>(null);

export function useBranding(): BrandingContextValue {
  const ctx = useContext(BrandingContext);
  if (!ctx) throw new Error('useBranding must be used within BrandingProvider');
  return ctx;
}
