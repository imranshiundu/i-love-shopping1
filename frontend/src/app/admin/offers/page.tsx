'use client';
import { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { admin, products as productsApi, categories as categoriesApi, brands as brandsApi } from '@/services/api';
import { Product, Category, Brand } from '@/types';
import { formatKES } from '@/lib/utils';
import Reveal from '@/components/ui/Reveal';
import { FiTag, FiZap, FiXCircle, FiPercent, FiChevronRight } from 'react-icons/fi';
import toast from 'react-hot-toast';

export default function AdminOffersPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);
  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);

  const [target, setTarget] = useState<'ALL' | 'CATEGORY' | 'BRAND'>('ALL');
  const [categoryId, setCategoryId] = useState('');
  const [brandId, setBrandId] = useState('');
  const [percent, setPercent] = useState('15');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [p, c, b] = await Promise.all([
        productsApi.search({ page: '0', size: '100' }),
        categoriesApi.list(),
        brandsApi.list(),
      ]);
      setProducts(p.data?.products || []);
      setCategories(c.data || []);
      setBrands(b.data || []);
    } catch (e: any) { toast.error(e.message); }
    setLoading(false);
  }, []);

  useEffect(() => { load(); }, [load]);

  const activeOffers = products.filter(p => p.compareAtPrice && p.compareAtPrice > p.price);

  const targetsFor = (): Product[] => {
    if (target === 'CATEGORY') return products.filter(p => p.category?.id === categoryId);
    if (target === 'BRAND') return products.filter(p => p.brand?.id === brandId);
    return products;
  };

  const createOffer = async () => {
    const pct = Number(percent);
    if (!pct || pct <= 0 || pct >= 95) { toast.error('Discount must be between 1 and 94 percent'); return; }
    const scope = targetsFor().filter(p => p.isActive);
    if (scope.length === 0) { toast.error('No active products match this target'); return; }

    setRunning(true);
    let ok = 0;
    for (const p of scope) {
      try {
        const base = p.compareAtPrice && p.compareAtPrice > p.price ? p.compareAtPrice : p.price;
        await admin.updateProduct(p.id, {
          name: p.name,
          description: p.description,
          price: Math.round(base * (1 - pct / 100) * 100) / 100,
          compareAtPrice: base,
          sku: p.sku,
          stock: p.stock,
          categoryId: p.category?.id,
          brandId: p.brand?.id,
          isActive: true,
        });
        ok++;
      } catch { /* keep going */ }
    }
    setRunning(false);
    toast.success(`Offer live on ${ok} of ${scope.length} products`);
    await load();
  };

  const endOffers = async () => {
    if (activeOffers.length === 0) return;
    if (!confirm(`End offers on ${activeOffers.length} products? Prices return to their original level.`)) return;
    setRunning(true);
    let ok = 0;
    for (const p of activeOffers) {
      try {
        await admin.updateProduct(p.id, {
          name: p.name,
          description: p.description,
          price: p.compareAtPrice!,
          compareAtPrice: null,
          sku: p.sku,
          stock: p.stock,
          categoryId: p.category?.id,
          brandId: p.brand?.id,
          isActive: true,
        });
        ok++;
      } catch { /* keep going */ }
    }
    setRunning(false);
    toast.success(`Ended offers on ${ok} products`);
    await load();
  };

  const totalSavingOnShelf = activeOffers.reduce(
    (sum, p) => sum + ((p.compareAtPrice || 0) - p.price), 0
  );

  return (
    <div className="space-y-6">
      <Reveal>
        <div>
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Offers and discounts</h1>
          <p className="mt-1 text-sm text-stone-500">Run store-wide or scoped promotions in one move.</p>
        </div>
      </Reveal>

      <div className="grid gap-6 xl:grid-cols-[1fr_1.2fr]">
        <Reveal delay={60}>
          <section className="rounded-2xl border border-stone-200/80 bg-white p-6">
            <h2 className="flex items-center gap-2 font-bold"><FiZap className="text-amber-500" /> Create an offer</h2>

            <fieldset className="mt-5">
              <legend className="mb-2 text-xs font-bold uppercase tracking-widest text-stone-400">Scope</legend>
              <div className="grid grid-cols-3 gap-2">
                {[
                  { id: 'ALL', label: 'Everything' },
                  { id: 'CATEGORY', label: 'Category' },
                  { id: 'BRAND', label: 'Brand' },
                ].map(t => (
                  <button key={t.id} onClick={() => setTarget(t.id as any)}
                    className={`rounded-xl border-2 px-3 py-2.5 text-sm font-semibold transition-colors ${
                      target === t.id ? 'border-primary-600 bg-primary-50 text-primary-700' : 'border-stone-200 hover:border-stone-300'
                    }`}>
                    {t.label}
                  </button>
                ))}
              </div>
            </fieldset>

            {target === 'CATEGORY' && (
              <select value={categoryId} onChange={e => setCategoryId(e.target.value)} className={selCls}>
                <option value="">Choose a category...</option>
                {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            )}
            {target === 'BRAND' && (
              <select value={brandId} onChange={e => setBrandId(e.target.value)} className={selCls}>
                <option value="">Choose a brand...</option>
                {brands.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
              </select>
            )}

            <fieldset className="mt-5">
              <legend className="mb-2 text-xs font-bold uppercase tracking-widest text-stone-400">Discount</legend>
              <div className="flex items-center gap-2">
                {[5, 10, 15, 25, 50].map(v => (
                  <button key={v} onClick={() => setPercent(String(v))}
                    className={`flex-1 rounded-xl py-2 text-sm font-bold transition-colors ${
                      percent === String(v) ? 'bg-stone-900 text-white' : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
                    }`}>
                    {v}%
                  </button>
                ))}
                <input type="number" min="1" max="94" value={percent}
                  onChange={e => setPercent(e.target.value)}
                  className="w-20 rounded-xl border border-stone-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100" />
              </div>
            </fieldset>

            <div className="mt-5 rounded-xl bg-stone-50 p-4 text-sm">
              <p className="text-stone-600">
                This will reprice <strong>{targetsFor().filter(p => p.isActive).length}</strong> active product{targetsFor().filter(p => p.isActive).length === 1 ? '' : 's'}.
              </p>
              {activeOffers.length > 0 && (
                <p className="mt-1 text-xs text-stone-400">Existing offers are rebased off their original price.</p>
              )}
            </div>

            <button onClick={createOffer} disabled={running}
              className="mt-4 flex w-full items-center justify-center gap-2 rounded-xl bg-primary-600 py-3 font-semibold text-white shadow-lg shadow-primary-600/25 transition-all hover:-translate-y-0.5 hover:bg-primary-700 disabled:opacity-60 disabled:hover:translate-y-0">
              {running ? 'Applying offer...' : <>Launch offer <FiPercent /></>}
            </button>
          </section>
        </Reveal>

        <Reveal delay={120}>
          <section className="rounded-2xl border border-stone-200/80 bg-white p-6">
            <div className="flex items-center justify-between">
              <h2 className="flex items-center gap-2 font-bold"><FiTag className="text-rose-500" /> Live offers</h2>
              <span className="rounded-full bg-rose-100 px-3 py-1 text-xs font-bold text-rose-700">{activeOffers.length}</span>
            </div>

            {loading ? (
              <div className="mt-4 space-y-2">{[...Array(4)].map((_, i) => <div key={i} className="h-10 animate-pulse rounded-lg bg-stone-200/50" />)}</div>
            ) : activeOffers.length === 0 ? (
              <p className="mt-6 rounded-xl border border-dashed border-stone-300 p-8 text-center text-sm text-stone-500">
                No offers running. Launch one to see it here.
              </p>
            ) : (
              <>
                <p className="mt-3 rounded-xl bg-emerald-50 p-3 text-xs font-medium text-emerald-800 ring-1 ring-emerald-200">
                  Shoppers see {formatKES(totalSavingOnShelf)} of total savings across these listings.
                </p>
                <ul className="mt-4 max-h-[320px] space-y-2 overflow-y-auto pr-1">
                  {activeOffers.map(p => (
                    <li key={p.id} className="flex items-center gap-3 rounded-xl border border-stone-100 p-2.5">
                      <span className="h-9 w-9 shrink-0 overflow-hidden rounded-lg bg-stone-100">
                        {p.images?.[0] && <img src={p.images[0].url} alt="" className="h-full w-full object-cover" />}
                      </span>
                      <Link href={`/products/${p.slug}`} className="min-w-0 flex-1 truncate text-sm font-medium hover:text-primary-700">
                        {p.name}
                        <FiChevronRight className="inline h-3 w-3" />
                      </Link>
                      <span className="text-xs text-stone-400 line-through">{formatKES(p.compareAtPrice!)}</span>
                      <span className="text-sm font-bold">{formatKES(p.price)}</span>
                      <span className="rounded-full bg-rose-100 px-2 py-0.5 text-[11px] font-bold text-rose-700">-{p.discountPercentage}%</span>
                    </li>
                  ))}
                </ul>
                <button onClick={endOffers} disabled={running}
                  className="mt-4 flex w-full items-center justify-center gap-2 rounded-xl border-2 border-rose-200 py-2.5 text-sm font-semibold text-rose-700 transition-colors hover:bg-rose-50 disabled:opacity-60">
                  <FiXCircle /> End all offers
                </button>
              </>
            )}
          </section>
        </Reveal>
      </div>
    </div>
  );
}

const selCls = 'mt-3 w-full rounded-xl border border-stone-300 px-3.5 py-2.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100';
