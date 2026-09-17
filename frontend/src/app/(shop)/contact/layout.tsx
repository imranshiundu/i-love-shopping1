import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Contact & support - i-love-shopping',
  description: 'Questions about an order or product? Send the team a message — replies within one business day.',
};

export default function ContactLayout({ children }: { children: React.ReactNode }) {
  return children;
}
