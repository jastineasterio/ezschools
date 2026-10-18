import { useBranding } from '@/store/BrandingContext';

export function useBrandingStatus() {
  const { branding, isLoading } = useBranding();
  return { branding, isLoading };
}
