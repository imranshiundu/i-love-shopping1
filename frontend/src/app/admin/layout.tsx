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
  FiArrowLeft, FiBarChart2,
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
    if (!loading && (!user || !user.roles?.includes('ADMIN'))) {
      router.push('/auth/login');
    }
  }, [user, loading, router]);

  if (loading) {
    return (
      <div className="flex min-h-[70vh] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-600 border-t-transparent" />
      </div>
    );
  }
  if (!user || !user.roles?.includes('ADMIN')) return null;

  return (
    <div className="mx-auto flex max-w-[1500px] gap-8 px-4 py-8 sm:px-6 lg:px-8">
      <aside className="hidden w-60 shrink-0 lg:block">
        <div className="sticky top-28 rounded-2xl border border-stone-200/80 bg-white p-4">
          <p className="flex items-center gap-2 px-3 pb-3 text-xs font-bold uppercase tracking-widest text-stone-400">
            <FiBarChart2 /> {config.app.name} admin
          </p>
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
            className="mt-4 flex items-center gap-2 border-t border-stone-100 px-3 pt-4 text-sm font-medium text-stone-500 hover:text-primary-600">
            <FiArrowLeft className="h-4 w-4" /> Back to store
          </Link>
        </div>
      </aside>

      <div className="min-w-0 flex-1">
        <nav className="no-scrollbar mb-6 flex gap-2 overflow-x-auto lg:hidden" aria-label="Admin sections">
          {NAV.map(item => (
            <Link key={item.href} href={item.href}
              className={cn(
                'flex shrink-0 items-center gap-1.5 rounded-full px-4 py-1.5 text-xs font-bold transition-colors',
                pathname === item.href ? 'bg-stone-900 text-white' : 'bg-white text-stone-600 ring-1 ring-stone-200'
              )}>
              <item.icon className="h-3.5 w-3.5" />
              {item.label}
            </Link>
          ))}
        </nav>
        {children}
      </div>
    </div>
  );
}
