import { ClerkProvider } from '@clerk/nextjs';
import type { Metadata } from 'next';
import { AuthProvider } from '@/contexts/AuthContext';
import { CurrencyProvider } from '@/lib/currency';
import { config } from '@/lib/config';
import ToastProvider from '@/components/ui/Toast';
import './globals.css';

export const metadata: Metadata = {
  title: `${process.env.NEXT_PUBLIC_APP_NAME || 'i-love-shopping'} - E-commerce Platform`,
  description: process.env.NEXT_PUBLIC_APP_DESCRIPTION || 'B2C E-commerce Platform for the Kenyan market',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  // Clerk (social sign-in) mounts only when a publishable key is configured —
  // a clean checkout with password auth must build and run with no Clerk setup.
  const app = (
    <AuthProvider>
      <CurrencyProvider>
        {children}
        {/* react-hot-toast outlet — without it every toast.error/success is invisible */}
        <ToastProvider />
      </CurrencyProvider>
    </AuthProvider>
  );
  return (
    <html lang="en">
      <body>
        {config.clerk.enabled ? <ClerkProvider>{app}</ClerkProvider> : app}
      </body>
    </html>
  );
}