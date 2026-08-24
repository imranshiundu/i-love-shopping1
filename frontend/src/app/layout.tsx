import type { Metadata } from 'next';
import { AuthProvider } from '@/contexts/AuthContext';
import Header from '@/components/layout/Header';
import Footer from '@/components/layout/Footer';
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
          <div className="min-h-screen flex flex-col">
            <Header />
            <main className="flex-1">{children}</main>
            <Footer />
          </div>
        </AuthProvider>
      </body>
    </html>
  );
}
