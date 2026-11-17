export type HexColor = string;

export type SchoolColors = {
  primaryColor: string;
  secondaryColor: string;
  accentColor: string;
  backgroundColor: string;
  textColor: string;
};

const HEX_REGEX = /^#([0-9A-Fa-f]{6})$/;

export function isValidHex(value: unknown): value is HexColor {
  return typeof value === 'string' && HEX_REGEX.test(value);
}

export function normalizeHex(value: string): HexColor | null {
  if (!isValidHex(value)) return null;
  return value.toLowerCase();
}

function hexToRgb(hex: string): { r: number; g: number; b: number } {
  const h = hex.slice(1);
  return {
    r: parseInt(h.slice(0, 2), 16),
    g: parseInt(h.slice(2, 4), 16),
    b: parseInt(h.slice(4, 6), 16),
  };
}

/** Tailwind-style HSL string: "H S% L%" */
export function hexToHsl(hex: string): string {
  const { r, g, b } = hexToRgb(hex);
  const rf = r / 255;
  const gf = g / 255;
  const bf = b / 255;

  const max = Math.max(rf, gf, bf);
  const min = Math.min(rf, gf, bf);
  const delta = max - min;

  let h = 0;
  if (delta !== 0) {
    if (max === rf) h = ((gf - bf) / delta) % 6;
    else if (max === gf) h = (bf - rf) / delta + 2;
    else h = (rf - gf) / delta + 4;
    h = Math.round(h * 60);
    if (h < 0) h += 360;
  }

  const l = (max + min) / 2;
  let s = 0;
  if (delta !== 0) {
    s = delta / (1 - Math.abs(2 * l - 1));
  }

  return `${h} ${Math.round(s * 100)}% ${Math.round(l * 100)}%`;
}

/** Relative luminance per WCAG (sRGB). */
export function getLuminance(hex: string): number {
  const { r, g, b } = hexToRgb(hex);
  const linear = (c: number) => {
    const f = c / 255;
    return f <= 0.03928 ? f / 12.92 : Math.pow((f + 0.055) / 1.055, 2.4);
  };
  return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b);
}

/** Contrast ratio between two colors. */
export function getContrast(a: string, b: string): number {
  const lumA = getLuminance(a);
  const lumB = getLuminance(b);
  const lighter = Math.max(lumA, lumB);
  const darker = Math.min(lumA, lumB);
  return (lighter + 0.05) / (darker + 0.05);
}

/** Pick 'white' or 'black' (as HSL strings) giving sufficient contrast on a background color. */
export function getContrastForeground(hex: string): string {
  const lum = getLuminance(hex);
  const contrastWhite = (lum + 0.05) / (0 + 0.05);
  const contrastBlack = (1 + 0.05) / (lum + 0.05);
  return contrastWhite >= contrastBlack ? '210 40% 98%' : '222.2 47.4% 11.2%';
}

/** Lighten or darken a hex color by a fraction (0-1). Negative darkens. */
export function adjustHex(hex: string, fraction: number): HexColor {
  const { r, g, b } = hexToRgb(hex);
  const shift = (c: number) => {
    const value = Math.round(c + (255 - c) * fraction);
    return Math.min(255, Math.max(0, value));
  };
  const nr = shift(r);
  const ng = shift(g);
  const nb = shift(b);
  return `#${nr.toString(16).padStart(2, '0')}${ng.toString(16).padStart(2, '0')}${nb.toString(16).padStart(2, '0')}`;
}

export const DEFAULT_BRANDING: SchoolColors = {
  primaryColor: '#3b82f6',
  secondaryColor: '#64748b',
  accentColor: '#f59e0b',
  backgroundColor: '#ffffff',
  textColor: '#0f172a',
};

export const THEME_PRESETS: Record<string, Record<string, HexColor>> = {
  'Professional Blue': {
    primaryColor: '#2563eb',
    secondaryColor: '#475569',
    accentColor: '#f59e0b',
    backgroundColor: '#ffffff',
    textColor: '#0f172a',
  },
  'Emerald Green': {
    primaryColor: '#059669',
    secondaryColor: '#334155',
    accentColor: '#d97706',
    backgroundColor: '#ffffff',
    textColor: '#0f172a',
  },
  'Royal Purple': {
    primaryColor: '#6d28d9',
    secondaryColor: '#3f3f46',
    accentColor: '#f59e0b',
    backgroundColor: '#ffffff',
    textColor: '#0f172a',
  },
  'Ocean Teal': {
    primaryColor: '#0f766e',
    secondaryColor: '#1e293b',
    accentColor: '#0891b2',
    backgroundColor: '#ffffff',
    textColor: '#0f172a',
  },
  Burgundy: {
    primaryColor: '#9f1232',
    secondaryColor: '#4c0519',
    accentColor: '#d97706',
    backgroundColor: '#ffffff',
    textColor: '#0f172a',
  },
  'Modern Neutral': {
    primaryColor: '#404040',
    secondaryColor: '#a1a1aa',
    accentColor: '#71717a',
    backgroundColor: '#ffffff',
    textColor: '#18181b',
  },
} as const;
