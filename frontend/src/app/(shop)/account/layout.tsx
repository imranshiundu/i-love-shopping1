'use client';
import { useEffect } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { cn } from '@/lib/utils';
import {
  FiGrid, FiShoppingBag, FiMapPin, FiSettings, FiArrowLeft, FiCreditCard,
} from 'react-icons/fi';

const NAV = [
  { href: '/account', label: 'Dashboard', icon: FiGrid },
  { href: '/account/orders', label: 'Orders', icon: FiShoppingBag },
  { href: '/account/payments', label: 'Payments', icon: FiCreditCard },
  { href: '/account/addresses', label: 'Addresses', icon: FiMapPin },
  { href: '/account/settings', label: 'Settings', icon: FiSettings },
];

export default function AccountLayout({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!loading && !user) {
      router.push('/auth/login?redirect=' + encodeURIComponent(pathname));
    }
  }, [user, loading, router, pathname]);

  if (loading) {
    return (
      <div className="flex min-h-[70vh] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-600 border-t-transparent" />
      </div>
    );
  }
  if (!user) return null;

  return (
    <div className="mx-auto flex max-w-[1400px] flex-col gap-8 px-4 py-10 sm:px-6 lg:flex-row lg:px-8">
      <aside className="shrink-0 lg:w-60">
        <div className="lg:sticky lg:top-32 rounded-2xl border border-stone-200/80 bg-white p-4">
          <div className="flex items-center gap-3 px-2 pb-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-stone-900 text-sm font-bold text-white">
              {user.name?.charAt(0).toUpperCase()}
            </span>
            <div className="min-w-0">
              <p className="truncate text-sm font-bold">{user.name}</p>
              <p className="truncate text-xs text-stone-400">{user.email}</p>
            </div>
          </div>
          <nav className="space-y-1">
            {NAV.map(item => {
              const active = pathname === item.href;
              return (
                <Link key={item.href} href={item.href}
                  className={cn(
                    'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors',
                    active ? 'bg-stone-900 text-white' : 'text-stone-600 hover:bg-stone-100'
                  )}>
                  <item.icon className="h-4 w-4" />
                  {item.label}
                </Link>
              );
            })}
          </nav>
          <Link href="/"
            className="mt-3 flex items-center gap-2 border-t border-stone-100 px-3 pt-3 text-sm font-medium text-stone-500 hover:text-primary-600">
            <FiArrowLeft className="h-4 w-4" /> Back to store
          </Link>
        </div>
      </aside>

      <div className="min-w-0 flex-1">
        <nav className="no-scrollbar mb-6 flex gap-2 overflow-x-auto lg:hidden">
          {NAV.map(item => (
            <Link key={item.href} href={item.href}
              className={cn(
                'flex shrink-0 items-center gap-1.5 rounded-full px-4 py-1.5 text-xs font-bold transition-colors',
                pathname === item.href ? 'bg-stone-900 text-white' : 'bg-white text-stone-600 ring-1 ring-stone-200'
              )}>
              <item.icon className="h-3 w-3" />
              {item.label}
            </Link>
          ))}
        </nav>
        {children}
      </div>
    </div>
  );
}
