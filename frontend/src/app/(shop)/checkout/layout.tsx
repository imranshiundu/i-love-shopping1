import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Checkout - i-love-shopping',
  description: 'Secure checkout: delivery options, addresses and payment with M-Pesa or card. Guest checkout supported.',
};

export default function CheckoutLayout({ children }: { children: React.ReactNode }) {
  return children;
}
