import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatTsh(value: number | string | null | undefined, compact = false) {
  const numericValue = Number(value ?? 0);

  if (!Number.isFinite(numericValue)) {
    return 'TSh 0';
  }

  const formatter = new Intl.NumberFormat('en-TZ', {
    style: 'currency',
    currency: 'TZS',
    currencyDisplay: 'narrowSymbol',
    notation: compact ? 'compact' : 'standard',
    maximumFractionDigits: compact ? 1 : 2,
    minimumFractionDigits: 0,
  });

  return formatter.format(numericValue).replace(/\s?TZS/, 'TSh');
}

export function formatTshCompact(value: number | string | null | undefined) {
  return formatTsh(value, true);
}
