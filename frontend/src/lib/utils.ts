import { config } from './config';
import { getActiveCurrency } from '@/lib/currency';
import { cart as cartApi, orders as ordersApi } from '@/services/api';
export { cartApi, ordersApi };

export function formatPrice(amountInKes: number): string {
  const currency = getActiveCurrency();
  const converted = amountInKes * currency.rate;
  const decimals = converted < 100 && currency.code !== 'KES' ? 2 : 0;
  return new Intl.NumberFormat(currency.locale, {
    style: 'currency',
    currency: currency.code,
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(converted);
}

export function formatKES(amount: number): string {
  return formatPrice(amount);
}

export function formatDate(date: string): string {
  return new Date(date).toLocaleDateString(config.commerce.locale, { year: 'numeric', month: 'short', day: 'numeric' });
}

export function cn(...classes: (string | boolean | undefined | null)[]): string {
  return classes.filter(Boolean).join(' ');
}

export function slugify(text: string): string {
  return text.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
}

export const ORDER_STATUSES = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED'] as const;

export function statusColor(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'bg-yellow-100 text-yellow-800', CONFIRMED: 'bg-blue-100 text-blue-800',
    PROCESSING: 'bg-indigo-100 text-indigo-800', SHIPPED: 'bg-purple-100 text-purple-800',
    DELIVERED: 'bg-green-100 text-green-800', CANCELLED: 'bg-red-100 text-red-800',
    REFUNDED: 'bg-gray-100 text-gray-800',
  };
  return map[status] || 'bg-gray-100 text-gray-800';
}
