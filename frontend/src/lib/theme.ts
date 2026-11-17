import {
  getContrastForeground,
  hexToHsl,
  normalizeHex,
  DEFAULT_BRANDING,
  type SchoolColors,
} from '@/lib/color';

export interface SchoolCssTokens {
  '--school-primary': string;
  '--school-primary-foreground': string;
  '--school-secondary': string;
  '--school-secondary-foreground': string;
  '--school-accent': string;
  '--school-accent-foreground': string;
  '--school-background': string;
  '--school-text': string;
  '--school-card': string;
  '--school-card-foreground': string;
  '--school-sidebar-background': string;
  '--school-sidebar-text': string;
  '--school-active-nav-background': string;
  '--school-active-nav-text': string;
  '--school-border': string;
  '--school-muted': string;
  '--school-muted-foreground': string;
  '--school-input': string;
  '--school-ring': string;
}

/** Default light-mode surface values used before branding loads and as fallbacks. */
const LIGHT_SURFACES: Partial<SchoolCssTokens> = {
  '--school-background': '210 40% 98%',
  '--school-card': '0 0% 100%',
  '--school-card-foreground': '222 47% 11%',
  '--school-sidebar-background': '0 0% 100%',
  '--school-sidebar-text': '222 47% 11%',
  '--school-active-nav-background': '221 83% 53%',
  '--school-active-nav-text': '210 40% 98%',
  '--school-border': '214 32% 91%',
  '--school-muted': '210 40% 96.1%',
  '--school-muted-foreground': '215 16% 47%',
  '--school-input': '0 0% 100%',
  '--school-ring': '221 83% 53%',
};

const DARK_SURFACES: Partial<SchoolCssTokens> = {
  '--school-background': '222 47% 11%',
  '--school-card': '217 33% 17%',
  '--school-card-foreground': '210 40% 98%',
  '--school-sidebar-background': '221 39% 11%',
  '--school-sidebar-text': '210 40% 98%',
  '--school-active-nav-background': '217 91% 60%',
  '--school-active-nav-text': '210 40% 98%',
  '--school-border': '215 25% 27%',
  '--school-muted': '217 33% 17%',
  '--school-muted-foreground': '213 27% 84%',
  '--school-input': '222 47% 11%',
  '--school-ring': '217 91% 60%',
};

function hslOrDefault(color: string | null | undefined, fallback: string): string {
  const hex = normalizeHex(color ?? '');
  return hex ? hexToHsl(hex) : fallback;
}

export function computeSchoolTokens(
  colors: SchoolColors | null,
  isDark: boolean,
): SchoolCssTokens {
  const c = colors ?? DEFAULT_BRANDING;
  const surfaces = isDark ? DARK_SURFACES : LIGHT_SURFACES;

  const primary = hslOrDefault(c.primaryColor, '#2563eb');
  const secondary = hslOrDefault(c.secondaryColor, '#64748b');
  const accent = hslOrDefault(c.accentColor, '#f59e0b');

  const primaryForeground = getContrastForeground(c.primaryColor ?? '#2563eb');
  const secondaryForeground = getContrastForeground(c.secondaryColor ?? '#64748b');
  const accentForeground = getContrastForeground(c.accentColor ?? '#f59e0b');

  /* Brand accent tokens come from the school colors. Surface tokens
     (background, card, sidebar, border, input, table-*) stay theme-driven so
     school branding never overrides the active light/dark chrome and never
     breaks readability. */
  return {
    '--school-primary': primary,
    '--school-primary-foreground': primaryForeground,
    '--school-secondary': secondary,
    '--school-secondary-foreground': secondaryForeground,
    '--school-accent': accent,
    '--school-accent-foreground': accentForeground,
    '--school-background': surfaces['--school-background']!,
    '--school-text': surfaces['--school-text']!,
    '--school-card': surfaces['--school-card']!,
    '--school-card-foreground': surfaces['--school-card-foreground']!,
    '--school-sidebar-background': surfaces['--school-sidebar-background']!,
    '--school-sidebar-text': surfaces['--school-sidebar-text']!,
    '--school-active-nav-background': primary,
    '--school-active-nav-text': primaryForeground,
    '--school-border': surfaces['--school-border']!,
    '--school-muted': surfaces['--school-muted']!,
    '--school-muted-foreground': surfaces['--school-muted-foreground']!,
    '--school-input': surfaces['--school-input']!,
    '--school-ring': primary,
  };
}

/** Applies school tokens to a DOM element. Returns the tokens applied. */
export function applySchoolTokens(
  element: HTMLElement,
  colors: SchoolColors | null,
  isDark: boolean,
): SchoolCssTokens {
  const tokens = computeSchoolTokens(colors, isDark);
  Object.entries(tokens).forEach(([key, value]) => {
    element.style.setProperty(key, value);
  });
  return tokens;
}

export { DEFAULT_BRANDING };
export type { HexColor, SchoolColors } from '@/lib/color';
