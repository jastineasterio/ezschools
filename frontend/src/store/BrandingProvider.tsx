import { useState, useEffect, useCallback, useRef } from 'react';
import { api } from '@/services/api';
import { useTheme } from '@/store/ThemeContext';
import { BrandingContext } from '@/store/BrandingContext';
import { useAuth } from '@/store/AuthContext';
import type { SchoolBrandingResponse } from '@/types/api';
import { applySchoolTokens, DEFAULT_BRANDING } from '@/lib/theme';

const CACHE_KEY_PREFIX = 'edumanage:school-branding:';

function cacheKeyFor(schoolId: number | null): string | null {
  return schoolId == null ? null : CACHE_KEY_PREFIX + schoolId;
}

export function BrandingProvider({ children }: { children: React.ReactNode }) {
  const { theme } = useTheme();
  const { user } = useAuth();
  const [branding, setBranding] = useState<SchoolBrandingResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const prevSchoolId = useRef<number | null>(null);

  const isDark = theme === 'dark';

  const applyTheme = useCallback(
    (data: SchoolBrandingResponse) => {
      applySchoolTokens(document.documentElement, data, isDark);
    },
    [isDark],
  );

  const schoolId = user?.schoolId ?? null;
  const cacheKey = cacheKeyFor(schoolId);

  useEffect(() => {
    // Platform / Super Admin has no school context: keep EduManage defaults and
    // never read or write a school's branding cache.
    if (!schoolId || !cacheKey) {
      setBranding(null);
      applySchoolTokens(document.documentElement, DEFAULT_BRANDING, isDark);
      setIsLoading(false);
      return;
    }

    let active = true;

    const run = async () => {
      try {
        const raw = sessionStorage.getItem(cacheKey);
        if (raw) {
          try {
            const cached = JSON.parse(raw) as SchoolBrandingResponse | null;
            if (cached && cached.schoolId === schoolId) {
              setBranding(cached);
              applySchoolTokens(document.documentElement, cached, isDark);
            }
          } catch {
            // Ignore malformed cache.
          }
        }

        const response = await api.get<{ data: SchoolBrandingResponse }>('/school/branding');
        if (!active) return;
        setBranding(response.data.data);
        try {
          sessionStorage.setItem(cacheKey, JSON.stringify(response.data.data));
        } catch {
          // Ignore quota errors.
        }
        applySchoolTokens(document.documentElement, response.data.data, isDark);
      } catch {
        if (!active) return;
        setBranding(null);
        applySchoolTokens(document.documentElement, DEFAULT_BRANDING, isDark);
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    };

    void run();

    return () => {
      active = false;
    };
  }, [schoolId, cacheKey, isDark, applyTheme]);

  // Mount and school change: clear stale cache for the previous school so a
  // different school's logo/colors never leak into the current session.
  useEffect(() => {
    const previous = prevSchoolId.current;
    if (previous != null && previous !== schoolId) {
      const prevKey = CACHE_KEY_PREFIX + previous;
      try {
        sessionStorage.removeItem(prevKey);
      } catch {
        // Ignore.
      }
    }
    prevSchoolId.current = schoolId;
  }, [schoolId]);

  // Re-apply the school theme whenever light/dark mode toggles.
  useEffect(() => {
    if (branding) {
      applySchoolTokens(document.documentElement, branding, isDark);
    } else {
      applySchoolTokens(document.documentElement, DEFAULT_BRANDING, isDark);
    }
  }, [branding, isDark]);

  const colors = branding
    ? {
        primaryColor: branding.primaryColor,
        secondaryColor: branding.secondaryColor,
        accentColor: branding.accentColor,
        backgroundColor: branding.backgroundColor,
        textColor: branding.textColor,
      }
    : DEFAULT_BRANDING;

  return (
    <BrandingContext.Provider
      value={{ branding, isLoading, setBranding, applyTheme, colors }}
    >
      {children}
    </BrandingContext.Provider>
  );
}
