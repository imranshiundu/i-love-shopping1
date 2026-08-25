'use client';
import { createContext, useContext, useEffect, useState, useCallback, Fragment, ReactNode } from 'react';
import { config } from '@/lib/config';

export interface Currency {
  code: string;
  symbol: string;
  rate: number;
  locale: string;
}

const DEFAULTS: Currency[] = [
  { code: 'KES', symbol: 'KSh', rate: 1, locale: 'en-KE' },
  { code: 'USD', symbol: '$', rate: 0.0077, locale: 'en-US' },
  { code: 'EUR', symbol: '\u20AC', rate: 0.0071, locale: 'de-DE' },
  { code: 'GBP', symbol: '\u00A3', rate: 0.0061, locale: 'en-GB' },
  { code: 'TZS', symbol: 'TSh', rate: 19.8, locale: 'sw-TZ' },
  { code: 'UGX', symbol: 'USh', rate: 28.3, locale: 'en-UG' },
  { code: 'ZAR', symbol: 'R', rate: 0.14, locale: 'en-ZA' },
];

function buildRates(): Currency[] {
  const raw = process.env.NEXT_PUBLIC_CURRENCY_RATES;
  if (!raw) return DEFAULTS;
  try {
    const overrides = new Map<string, number>();
    for (const pair of raw.split(',')) {
      const [code, rate] = pair.trim().split('=');
      if (code && rate && !Number.isNaN(Number(rate))) overrides.set(code.toUpperCase(), Number(rate));
    }
    return DEFAULTS.map(c => (overrides.has(c.code) ? { ...c, rate: overrides.get(c.code)! } : c));
  } catch {
    return DEFAULTS;
  }
}

export const CURRENCIES: Currency[] = buildRates();

let activeCurrency: Currency = CURRENCIES[0];

export function getActiveCurrency(): Currency {
  return activeCurrency;
}

interface CurrencyContextType {
  currency: Currency;
  setCurrencyCode: (code: string) => void;
}

const CurrencyContext = createContext<CurrencyContextType | undefined>(undefined);

export function CurrencyProvider({ children }: { children: ReactNode }) {
  const [currency, setCurrency] = useState<Currency>(activeCurrency);

  useEffect(() => {
    const saved = typeof window !== 'undefined' ? localStorage.getItem('currency') : null;
    const match = CURRENCIES.find(c => c.code === saved);
    if (match) {
      activeCurrency = match;
      setCurrency(match);
    }
  }, []);

  const setCurrencyCode = useCallback((code: string) => {
    const match = CURRENCIES.find(c => c.code === code);
    if (!match) return;
    activeCurrency = match;
    setCurrency(match);
    localStorage.setItem('currency', code);
  }, []);

  return (
    <CurrencyContext.Provider value={{ currency, setCurrencyCode }}>
      <Fragment key={currency.code}>{children}</Fragment>
    </CurrencyContext.Provider>
  );
}
export function useCurrency() {
  const ctx = useContext(CurrencyContext);
  if (!ctx) throw new Error('useCurrency must be used within CurrencyProvider');
  return ctx;
}
