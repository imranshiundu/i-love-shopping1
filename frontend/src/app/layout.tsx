import type { Metadata } from 'next';
import { AuthProvider } from '@/contexts/AuthContext';
import { CurrencyProvider } from '@/lib/currency';
import './globals.css';

export const metadata: Metadata = {
  title: `${process.env.NEXT_PUBLIC_APP_NAME || 'i-love-shopping'} - E-commerce Platform`,
  description: process.env.NEXT_PUBLIC_APP_DESCRIPTION || 'B2C E-commerce Platform for the Kenyan market',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <AuthProvider>
          <CurrencyProvider>{children}</CurrencyProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
