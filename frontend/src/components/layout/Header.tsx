'use client';
import { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { useAuth } from '@/contexts/AuthContext';
import { products as productsApi } from '@/services/api';
import { config } from '@/lib/config';
import { formatKES } from '@/lib/utils';
import { FiShoppingCart, FiUser, FiSearch, FiMenu, FiX, FiTruck } from 'react-icons/fi';

export default function Header() {
  const { user, logout, cartCount } = useAuth();
  const [searchQuery, setSearchQuery] = useState('');
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (searchQuery.length >= 2) {
      const t = setTimeout(() => {
        productsApi.getSuggestions(searchQuery).then(res => setSuggestions(res.data || [])).catch(() => setSuggestions([]));
      }, 180);
      return () => clearTimeout(t);
    }
    setSuggestions([]);
    return undefined;
  }, [searchQuery]);

  useEffect(() => {
    const handleClick = (e: MouseEvent) => { if (searchRef.current && !searchRef.current.contains(e.target as Node)) setSuggestions([]); };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) window.location.href = `/products?q=${encodeURIComponent(searchQuery.trim())}`;
  };

  return (
    <header className="sticky top-0 z-50">
      <div className="bg-stone-950 py-2 text-center text-[13px] font-medium text-stone-200">
        <span className="inline-flex items-center gap-2">
          <FiTruck className="h-3.5 w-3.5 text-primary-300" />
          Complimentary delivery on orders over {formatKES(config.commerce.freeShippingThreshold)}
        </span>
      </div>

      <div className="border-b border-stone-200/70 bg-white/85 backdrop-blur-xl">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex h-[4.5rem] items-center justify-between gap-6">
            <Link href="/" className="flex items-baseline gap-1 whitespace-nowrap">
              <span className="text-xl font-extrabold tracking-tight text-stone-900">{config.app.name}</span>
              <span className="hidden text-xl font-extrabold tracking-tight text-primary-600 sm:inline">.</span>
            </Link>

            <nav className="hidden items-center gap-8 text-sm font-medium text-stone-600 lg:flex">
              <Link href="/products" className="transition-colors hover:text-stone-950">Shop all</Link>
              <Link href="/products?sortBy=newest" className="transition-colors hover:text-stone-950">New in</Link>
              <Link href="/products?onSaleOnly=true" className="transition-colors hover:text-rose-700">Offers</Link>
            </nav>

            <div className="hidden max-w-md flex-1 md:block" ref={searchRef}>
              <form onSubmit={handleSearch} className="relative">
                <input
                  type="text" value={searchQuery} onChange={e => setSearchQuery(e.target.value)}
                  placeholder="Search the collection"
                  className="w-full rounded-full border border-stone-300 bg-stone-50/60 py-2.5 pl-11 pr-4 text-sm transition-all placeholder:text-stone-400 focus:border-primary-400 focus:bg-white focus:outline-none focus:ring-4 focus:ring-primary-100"
                />
                <FiSearch className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-stone-400" />
                {suggestions.length > 0 && (
                  <div className="absolute top-full mt-2 w-full overflow-hidden rounded-2xl border border-stone-200 bg-white shadow-xl shadow-stone-900/5">
                    {suggestions.slice(0, 6).map((s, i) => (
                      <Link key={i} href={`/products?q=${encodeURIComponent(s)}`}
                        onClick={() => setSearchQuery('')}
                        className="block px-4 py-2.5 text-sm text-stone-600 transition-colors hover:bg-stone-50 hover:text-stone-950">
                        {s}
                      </Link>
                    ))}
                  </div>
                )}
              </form>
            </div>

            <div className="flex items-center gap-1">
              <Link href="/cart" className="group relative rounded-full p-2.5 text-stone-600 transition-colors hover:bg-stone-100 hover:text-stone-950" aria-label="Cart">
                <FiShoppingCart className="h-5 w-5" />
                {cartCount > 0 && (
                  <span className="absolute -right-0.5 -top-0.5 flex h-5 w-5 items-center justify-center rounded-full bg-primary-600 text-[10px] font-bold text-white ring-2 ring-white">
                    {cartCount}
                  </span>
                )}
              </Link>

              {user ? (
                <div className="relative">
                  <button onClick={() => setUserMenuOpen(!userMenuOpen)}
                    className="ml-1 flex items-center gap-2 rounded-full p-1 pr-3 transition-colors hover:bg-stone-100" aria-label="Account menu">
                    <span className="flex h-8 w-8 items-center justify-center rounded-full bg-stone-900 text-sm font-bold text-white">
                      {user.name.charAt(0).toUpperCase()}
                    </span>
                    <span className="hidden max-w-[7rem] truncate text-sm font-medium sm:inline">{user.name.split(' ')[0]}</span>
                  </button>
                  {userMenuOpen && (
                    <>
                      <div className="fixed inset-0 z-40" onClick={() => setUserMenuOpen(false)} />
                      <div className="absolute right-0 z-50 mt-2 w-56 overflow-hidden rounded-2xl border border-stone-200 bg-white py-1.5 shadow-xl shadow-stone-900/10">
                        <div className="border-b border-stone-100 px-4 py-3">
                          <p className="text-sm font-semibold">{user.name}</p>
                          <p className="truncate text-xs text-stone-500">{user.email}</p>
                        </div>
                        <Link href="/account" onClick={() => setUserMenuOpen(false)} className="block px-4 py-2.5 text-sm hover:bg-stone-50">My account</Link>
                        <Link href="/account/orders" onClick={() => setUserMenuOpen(false)} className="block px-4 py-2.5 text-sm hover:bg-stone-50">Orders</Link>
                        <Link href="/account/addresses" onClick={() => setUserMenuOpen(false)} className="block px-4 py-2.5 text-sm hover:bg-stone-50">Addresses</Link>
                        {user.roles?.includes('ADMIN') && (
                          <Link href="/admin" onClick={() => setUserMenuOpen(false)} className="block px-4 py-2.5 text-sm font-semibold text-primary-700 hover:bg-primary-50">Admin console</Link>
                        )}
                        <button onClick={() => { logout(); setUserMenuOpen(false); }}
                          className="block w-full border-t border-stone-100 px-4 py-2.5 text-left text-sm text-rose-600 hover:bg-rose-50">Sign out</button>
                      </div>
                    </>
                  )}
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <Link href="/auth/login" className="rounded-full px-3.5 py-2 text-sm font-semibold text-stone-700 transition-colors hover:bg-stone-100">
                    Sign in
                  </Link>
                  <Link href="/auth/register" className="hidden rounded-full bg-stone-900 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-stone-800 sm:block">
                    Create account
                  </Link>
                </div>
              )}

              <button onClick={() => setMobileMenuOpen(!mobileMenuOpen)} className="rounded-full p-2.5 text-stone-600 hover:bg-stone-100 lg:hidden" aria-label="Menu">
                {mobileMenuOpen ? <FiX className="h-5 w-5" /> : <FiMenu className="h-5 w-5" />}
              </button>
            </div>
          </div>
        </div>

        {mobileMenuOpen && (
          <div className="border-t border-stone-100 bg-white px-4 pb-5 pt-3 lg:hidden">
            <form onSubmit={handleSearch} className="relative mb-4 md:hidden">
              <input type="text" value={searchQuery} onChange={e => setSearchQuery(e.target.value)}
                placeholder="Search the collection"
                className="w-full rounded-full border border-stone-300 bg-stone-50 py-2.5 pl-10 pr-4 text-sm focus:border-primary-400 focus:outline-none focus:ring-4 focus:ring-primary-100" />
              <FiSearch className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-stone-400" />
            </form>
            <nav className="space-y-0.5 text-[15px]">
              {[['Shop all', '/products'], ['New in', '/products?sortBy=newest'], ['Offers', '/products?onSaleOnly=true'], ['Cart', '/cart']].map(([label, href]) => (
                <Link key={href} href={href} onClick={() => setMobileMenuOpen(false)}
                  className="flex items-center justify-between rounded-xl px-3 py-2.5 font-medium text-stone-700 hover:bg-stone-50">
                  {label}
                </Link>
              ))}
            </nav>
          </div>
        )}
      </div>
    </header>
  );
}
