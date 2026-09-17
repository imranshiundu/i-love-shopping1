import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Your cart - i-love-shopping',
  description: 'Review your cart, update quantities and checkout securely with M-Pesa or card.',
};

export default function CartLayout({ children }: { children: React.ReactNode }) {
  return children;
}
