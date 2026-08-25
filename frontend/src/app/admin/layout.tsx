'use client';
import { useEffect } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { config } from '@/lib/config';
import { cn } from '@/lib/utils';
import {
  FiGrid, FiShoppingBag, FiPackage, FiTag, FiFolder, FiBookmark, FiUsers,
  FiArrowLeft, FiBarChart2, FiAlertTriangle,
} from 'react-icons/fi';

const NAV = [
  { href: '/admin', label: 'Dashboard', icon: FiGrid },
  { href: '/admin/orders', label: 'Orders', icon: FiShoppingBag },
  { href: '/admin/products', label: 'Products', icon: FiPackage },
  { href: '/admin/offers', label: 'Offers', icon: FiTag },
  { href: '/admin/categories', label: 'Categories', icon: FiFolder },
  { href: '/admin/brands', label: 'Brands', icon: FiBookmark },
  { href: '/admin/users', label: 'Customers', icon: FiUsers },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
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
      <div className="flex min-h-[100dvh] items-center justify-center bg-stone-950">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-600 border-t-transparent" />
      </div>
    );
  }

  if (!user) return null;

  if (!user.roles?.includes('ADMIN')) {
    return (
      <div className="flex min-h-[100dvh] items-center justify-center bg-stone-950 px-4">
        <div className="max-w-md rounded-3xl border border-white/10 bg-white/5 p-10 text-center backdrop-blur">
          <span className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-rose-500/15 text-rose-400 ring-1 ring-rose-500/30">
            <FiAlertTriangle className="h-7 w-7" />
          </span>
          <h1 className="mt-6 text-2xl font-bold text-white">Restricted area</h1>
          <p className="mt-2 leading-relaxed text-stone-400">
            This console is for store administrators only.
            Your account does not have the required role.
          </p>
          <Link href="/"
            className="mt-7 inline-flex items-center justify-center rounded-xl bg-white px-6 py-3 font-semibold text-stone-900 transition-colors hover:bg-primary-50">
            Back to storefront
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-[100dvh] bg-stone-100">
      <aside className="sticky top-0 hidden h-[100dvh] w-64 shrink-0 flex-col border-r border-stone-800 bg-stone-950 lg:flex">
        <div className="flex items-center gap-2.5 px-6 py-6">
          <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary-600 text-white">
            <FiBarChart2 className="h-5 w-5" />
          </span>
          <div>
            <p className="font-bold text-white">{config.app.name}</p>
            <p className="text-xs text-stone-500">Management console</p>
          </div>
        </div>

        <nav className="flex-1 space-y-1 px-3 py-2">
          {NAV.map(item => {
            const active = pathname === item.href;
            return (
              <Link key={item.href} href={item.href}
                className={cn(
                  'flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-medium transition-colors',
                  active
                    ? 'bg-primary-600 text-white shadow-lg shadow-primary-600/25'
                    : 'text-stone-400 hover:bg-white/5 hover:text-white'
                )}>
                <item.icon className="h-4 w-4" />
                {item.label}
                {active && <span className="ml-auto h-1.5 w-1.5 rounded-full bg-white/70" />}
              </Link>
            );
          })}
        </nav>

        <div className="border-t border-white/5 p-4">
          <Link href="/"
            className="flex items-center gap-2 rounded-xl px-3 py-2.5 text-sm font-medium text-stone-400 transition-colors hover:bg-white/5 hover:text-white">
            <FiArrowLeft className="h-4 w-4" /> Back to store
          </Link>
          <div className="mt-2 flex items-center gap-2.5 rounded-xl bg-white/5 px-3 py-2.5">
            <span className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-500/20 text-sm font-bold text-primary-300">
              {user.name?.charAt(0).toUpperCase()}
            </span>
            <div className="min-w-0">
              <p className="truncate text-xs font-semibold text-white">{user.name}</p>
              <p className="truncate text-[11px] text-stone-500">{user.email}</p>
            </div>
          </div>
        </div>
      </aside>

      <div className="min-w-0 flex-1">
        <header className="sticky top-0 z-40 flex items-center gap-3 border-b border-stone-200 bg-white/90 px-4 py-3 backdrop-blur lg:hidden">
          <Link href="/" className="rounded-lg p-2 hover:bg-stone-100" aria-label="Back to store"><FiArrowLeft /></Link>
          <nav className="no-scrollbar flex flex-1 gap-2 overflow-x-auto" aria-label="Admin sections">
            {NAV.map(item => (
              <Link key={item.href} href={item.href}
                className={cn(
                  'flex shrink-0 items-center gap-1.5 rounded-full px-3.5 py-1.5 text-xs font-bold transition-colors',
                  pathname === item.href ? 'bg-primary-600 text-white' : 'bg-stone-100 text-stone-600'
                )}>
                <item.icon className="h-3 w-3" />
                {item.label}
              </Link>
            ))}
          </nav>
        </header>

        <main className="min-w-0 p-4 sm:p-6 lg:p-8">{children}</main>
      </div>
    </div>
  );
}
