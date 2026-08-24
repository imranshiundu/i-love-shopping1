'use client';
import { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { useAuth } from '@/contexts/AuthContext';
import { products as productsApi } from '@/services/api';
import { config } from '@/lib/config';
import { FiShoppingCart, FiUser, FiSearch, FiMenu, FiX, FiPackage, FiLogOut, FiSettings } from 'react-icons/fi';

export default function Header() {
  const { user, logout, cartCount } = useAuth();
  const [searchQuery, setSearchQuery] = useState('');
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (searchQuery.length >= 2) {
      productsApi.getSuggestions(searchQuery).then(res => setSuggestions(res.data || [])).catch(() => setSuggestions([]));
    } else { setSuggestions([]); }
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
    <header className="bg-white shadow-sm sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <Link href="/" className="text-2xl font-bold text-primary-600">{config.app.name}</Link>

          <div className="hidden md:flex flex-1 max-w-lg mx-8" ref={searchRef}>
            <form onSubmit={handleSearch} className="w-full relative">
              <input type="text" value={searchQuery} onChange={e => setSearchQuery(e.target.value)}
                placeholder="Search products..." className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500" />
              <FiSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              {suggestions.length > 0 && (
                <div className="absolute top-full mt-1 w-full bg-white border border-gray-200 rounded-lg shadow-lg">
                  {suggestions.map((s, i) => (
                    <Link key={i} href={`/products?q=${encodeURIComponent(s)}`}
                      className="block px-4 py-2 hover:bg-gray-50 text-sm">{s}</Link>
                  ))}
                </div>
              )}
            </form>
          </div>

          <div className="flex items-center gap-4">
            <Link href="/cart" className="relative p-2 text-gray-600 hover:text-primary-600">
              <FiShoppingCart className="h-6 w-6" />
              {cartCount > 0 && <span className="absolute -top-1 -right-1 bg-primary-600 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center">{cartCount}</span>}
            </Link>

            {user ? (
              <div className="relative">
                <button onClick={() => setUserMenuOpen(!userMenuOpen)} className="flex items-center gap-2 text-gray-600 hover:text-primary-600">
                  <div className="h-8 w-8 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 font-medium">{user.name.charAt(0).toUpperCase()}</div>
                </button>
                {userMenuOpen && (
                  <div className="absolute right-0 mt-2 w-48 bg-white border border-gray-200 rounded-lg shadow-lg py-1 z-50">
                    <div className="px-4 py-2 border-b"><p className="text-sm font-medium">{user.name}</p><p className="text-xs text-gray-500">{user.email}</p></div>
                    <Link href="/account" className="block px-4 py-2 text-sm hover:bg-gray-50" onClick={() => setUserMenuOpen(false)}>My Account</Link>
                    <Link href="/account/orders" className="block px-4 py-2 text-sm hover:bg-gray-50" onClick={() => setUserMenuOpen(false)}>Orders</Link>
                    <Link href="/account/addresses" className="block px-4 py-2 text-sm hover:bg-gray-50" onClick={() => setUserMenuOpen(false)}>Addresses</Link>
                    {user.roles?.includes('ADMIN') && <Link href="/admin" className="block px-4 py-2 text-sm hover:bg-gray-50 text-primary-600" onClick={() => setUserMenuOpen(false)}>Admin Dashboard</Link>}
                    <button onClick={() => { logout(); setUserMenuOpen(false); }} className="block w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-gray-50">Sign Out</button>
                  </div>
                )}
              </div>
            ) : (
              <Link href="/auth/login" className="flex items-center gap-2 text-gray-600 hover:text-primary-600">
                <FiUser className="h-5 w-5" /><span className="hidden sm:inline text-sm">Sign In</span>
              </Link>
            )}

            <button onClick={() => setMobileMenuOpen(!mobileMenuOpen)} className="md:hidden p-2 text-gray-600">
              {mobileMenuOpen ? <FiX className="h-6 w-6" /> : <FiMenu className="h-6 w-6" />}
            </button>
          </div>
        </div>

        <nav className="hidden md:flex gap-6 pb-2 text-sm">
          <Link href="/products" className="text-gray-600 hover:text-primary-600">All Products</Link>
          <Link href="/products?sortBy=newest" className="text-gray-600 hover:text-primary-600">New Arrivals</Link>
          <Link href="/products?onSaleOnly=true" className="text-gray-600 hover:text-primary-600">On Sale</Link>
        </nav>
      </div>

      {mobileMenuOpen && (
        <div className="md:hidden border-t bg-white px-4 py-3 space-y-2">
          <form onSubmit={handleSearch} className="relative mb-3">
            <input type="text" value={searchQuery} onChange={e => setSearchQuery(e.target.value)} placeholder="Search..." className="w-full pl-9 pr-4 py-2 border rounded-lg" />
            <FiSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          </form>
          <Link href="/products" className="block py-2" onClick={() => setMobileMenuOpen(false)}>All Products</Link>
          <Link href="/products?sortBy=newest" className="block py-2" onClick={() => setMobileMenuOpen(false)}>New Arrivals</Link>
          <Link href="/products?onSaleOnly=true" className="block py-2" onClick={() => setMobileMenuOpen(false)}>On Sale</Link>
          <Link href="/cart" className="block py-2" onClick={() => setMobileMenuOpen(false)}>Cart ({cartCount})</Link>
        </div>
      )}
    </header>
  );
}
