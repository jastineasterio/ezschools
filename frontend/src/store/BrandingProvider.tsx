import { useState, useEffect } from 'react';
import { api } from '@/services/api';
import type { SchoolBrandingResponse } from '@/types/api';
import { BrandingContext } from '@/store/BrandingContext';

export function BrandingProvider({ children }: { children: React.ReactNode }) {
  const [branding, setBranding] = useState<SchoolBrandingResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchBranding = async () => {
      try {
        const { data } = await api.get<{ data: SchoolBrandingResponse }>('/school/branding');
        setBranding(data.data);
      } catch {
        // ignore - no branding available
      } finally {
        setIsLoading(false);
      }
    };
    fetchBranding();
  }, []);

  const applyTheme = (brandingData: SchoolBrandingResponse) => {
    const root = document.documentElement;
    const setVar = (key: string, value: string) => {
      root.style.setProperty(key, value);
    };
    setVar('--primary', brandingData.primaryColor);
    setVar('--secondary', brandingData.secondaryColor);
    setVar('--accent', brandingData.accentColor);
  };

  return (
    <BrandingContext.Provider value={{ branding, isLoading, setBranding, applyTheme }}>
      {children}
    </BrandingContext.Provider>
  );
}
