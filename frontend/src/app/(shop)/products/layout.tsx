import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'All products - i-love-shopping',
  description:
    'Browse the full catalogue: home goods, decor, plants and more. Filter by category, brand and price — free delivery on larger orders.',
};

export default function ProductsLayout({ children }: { children: React.ReactNode }) {
  return children;
}
