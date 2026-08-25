'use client';
import { useState, useEffect, useCallback, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { products as productsApi, categories as categoriesApi, brands as brandsApi } from '@/services/api';
import { config } from '@/lib/config';
import { Product, Category, Brand } from '@/types';
import { cn } from '@/lib/utils';
import Reveal from '@/components/ui/Reveal';
import ProductCard from '@/components/product/ProductCard';
import { FiFilter, FiX, FiChevronLeft, FiChevronRight, FiSearch } from 'react-icons/fi';

function ProductsContent() {
  const searchParams = useSearchParams();
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);
  const [loading, setLoading] = useState(true);
  const [pagination, setPagination] = useState({ page: 0, size: config.pages.productsPageSize, totalElements: 0, totalPages: 0 });
  const [showFilters, setShowFilters] = useState(false);

  const [query, setQueryInput] = useState(searchParams.get('q') || '');
  const [selectedCategory, setSelectedCategory] = useState(searchParams.get('category') || '');
  const [selectedBrand, setSelectedBrand] = useState(searchParams.get('brand') || '');
  const [minPrice, setMinPrice] = useState(searchParams.get('minPrice') || '');
  const [maxPrice, setMaxPrice] = useState(searchParams.get('maxPrice') || '');
  const [sortBy, setSortBy] = useState(searchParams.get('sortBy') || 'relevance');
  const [inStockOnly, setInStockOnly] = useState(searchParams.get('inStockOnly') === 'true');
  const [onSaleOnly, setOnSaleOnly] = useState(searchParams.get('onSaleOnly') === 'true');
  const [currentPage, setCurrentPage] = useState(parseInt(searchParams.get('page') || '0'));

  const fetchProducts = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string> = { page: String(currentPage), size: String(config.pages.productsPageSize), sortBy };
      if (query) params.query = query;
      if (selectedCategory) params.categories = selectedCategory;
      if (selectedBrand) params.brands = selectedBrand;
      if (minPrice) params.minPrice = minPrice;
      if (maxPrice) params.maxPrice = maxPrice;
      if (inStockOnly) params.inStockOnly = 'true';
      if (onSaleOnly) params.onSaleOnly = 'true';
      const res = await productsApi.search(params);
      if (res.data) {
        setProducts(res.data.products || []);
        setPagination(res.data.pagination);
      }
    } catch { setProducts([]); }
    setLoading(false);
  }, [query, selectedCategory, selectedBrand, minPrice, maxPrice, sortBy, inStockOnly, onSaleOnly, currentPage]);

  useEffect(() => {
    Promise.all([categoriesApi.list(), brandsApi.list()]).then(([c, b]) => {
      setCategories(c.data || []);
      setBrands(b.data || []);
    });
  }, []);

  useEffect(() => { fetchProducts(); }, [fetchProducts]);

  const clearFilters = () => {
    setQueryInput(''); setSelectedCategory(''); setSelectedBrand(''); setMinPrice(''); setMaxPrice('');
    setInStockOnly(false); setOnSaleOnly(false); setCurrentPage(0);
  };

  const hasFilters = query || selectedCategory || selectedBrand || minPrice || maxPrice || inStockOnly || onSaleOnly;

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="mb-8">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary-600">Catalogue</p>
        <h1 className="mt-1.5 text-3xl font-bold tracking-tight sm:text-4xl">
          {query ? <>Results for &ldquo;{query}&rdquo;</> : 'All products'}
        </h1>
        <p className="mt-1.5 text-sm text-stone-500">{pagination.totalElements} products found</p>
      </div>

      <div className="flex flex-col gap-8 lg:flex-row">
        <FilterSidebar
          categories={categories} brands={brands}
          query={query} setQuery={setQueryInput}
          selectedCategory={selectedCategory} setSelectedCategory={v => { setSelectedCategory(v); setCurrentPage(0); }}
          selectedBrand={selectedBrand} setSelectedBrand={v => { setSelectedBrand(v); setCurrentPage(0); }}
          minPrice={minPrice} setMinPrice={setMinPrice}
          maxPrice={maxPrice} setMaxPrice={setMaxPrice}
          inStockOnly={inStockOnly} setInStockOnly={v => { setInStockOnly(v); setCurrentPage(0); }}
          onSaleOnly={onSaleOnly} setOnSaleOnly={v => { setOnSaleOnly(v); setCurrentPage(0); }}
          clearFilters={clearFilters} hasFilters={!!hasFilters}
          showFilters={showFilters} onClose={() => setShowFilters(false)}
        />

        <div className="min-w-0 flex-1">
          <div className="mb-6 flex items-center justify-between gap-3">
            <button onClick={() => setShowFilters(!showFilters)} className="flex items-center gap-2 rounded-xl border border-stone-300 px-4 py-2.5 text-sm font-medium hover:border-stone-400 lg:hidden">
              <FiFilter className="h-4 w-4" /> Filters{hasFilters ? ' - on' : ''}
            </button>
            <label className="ml-auto flex items-center gap-2 text-sm text-stone-500">
              Sort
              <select
                value={sortBy}
                onChange={e => { setSortBy(e.target.value); setCurrentPage(0); }}
                className="rounded-xl border border-stone-300 bg-white px-3 py-2.5 text-sm font-medium text-stone-900 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100"
              >
                <option value="relevance">Relevance</option>
                <option value="price_asc">Price: low to high</option>
                <option value="price_desc">Price: high to low</option>
                <option value="newest">Newest</option>
                <option value="rating">Top rated</option>
              </select>
            </label>
          </div>

          {loading ? (
            <div className="grid grid-cols-2 gap-5 xl:grid-cols-3">
              {[...Array(9)].map((_, i) => (
                <div key={i} className="overflow-hidden rounded-2xl border border-stone-100">
                  <div className="aspect-[4/5] animate-pulse bg-stone-200/70" />
                  <div className="space-y-2 p-4">
                    <div className="h-3 w-1/3 animate-pulse rounded bg-stone-200/70" />
                    <div className="h-4 w-3/4 animate-pulse rounded bg-stone-200/70" />
                    <div className="h-4 w-1/4 animate-pulse rounded bg-stone-200/70" />
                  </div>
                </div>
              ))}
            </div>
          ) : products.length === 0 ? (
            <div className="rounded-3xl border border-dashed border-stone-300 px-6 py-20 text-center">
              <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-stone-100">
                <FiSearch className="h-6 w-6 text-stone-400" />
              </span>
              <p className="mt-5 text-lg font-semibold text-stone-800">Nothing matches those filters</p>
              <p className="mt-1 text-sm text-stone-500">Try widening the price range or clearing a filter.</p>
              {hasFilters && (
                <button onClick={clearFilters} className="mt-6 rounded-xl bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-700">
                  Clear all filters
                </button>
              )}
            </div>
          ) : (
            <>
              <div className="grid grid-cols-2 gap-5 xl:grid-cols-3">
                {products.map((product, i) => (
                  <Reveal key={product.id} delay={(i % 3) * 80}>
                    <ProductCard product={product} />
                  </Reveal>
                ))}
              </div>

              {pagination.totalPages > 1 && (
                <nav className="mt-12 flex items-center justify-center gap-2" aria-label="Pagination">
                  <button onClick={() => setCurrentPage(p => Math.max(0, p - 1))} disabled={currentPage === 0}
                    className="rounded-lg border border-stone-300 p-2.5 hover:border-stone-400 disabled:opacity-40" aria-label="Previous page">
                    <FiChevronLeft />
                  </button>
                  {Array.from({ length: Math.min(5, pagination.totalPages) }, (_, i) => {
                    const start = Math.max(0, Math.min(currentPage - 2, pagination.totalPages - 5));
                    return start + i;
                  }).filter(p => p >= 0 && p < pagination.totalPages).map(p => (
                    <button key={p} onClick={() => setCurrentPage(p)} aria-label={`Page ${p + 1}`}
                      className={cn(
                        'h-11 w-11 rounded-lg border text-sm font-semibold transition-colors',
                        p === currentPage ? 'border-primary-600 bg-primary-600 text-white' : 'border-stone-300 hover:border-stone-400'
                      )}>
                      {p + 1}
                    </button>
                  ))}
                  <button onClick={() => setCurrentPage(p => Math.min(pagination.totalPages - 1, p + 1))} disabled={currentPage >= pagination.totalPages - 1}
                    className="rounded-lg border border-stone-300 p-2.5 hover:border-stone-400 disabled:opacity-40" aria-label="Next page">
                    <FiChevronRight />
                  </button>
                </nav>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

interface FilterProps {
  categories: Category[]; brands: Brand[];
  query: string; setQuery: (v: string) => void;
  selectedCategory: string; setSelectedCategory: (v: string) => void;
  selectedBrand: string; setSelectedBrand: (v: string) => void;
  minPrice: string; setMinPrice: (v: string) => void;
  maxPrice: string; setMaxPrice: (v: string) => void;
  inStockOnly: boolean; setInStockOnly: (v: boolean) => void;
  onSaleOnly: boolean; setOnSaleOnly: (v: boolean) => void;
  clearFilters: () => void; hasFilters: boolean;
  showFilters: boolean; onClose: () => void;
}

function FilterSidebar(p: FilterProps) {
  return (
    <aside className={cn(
      'shrink-0 lg:block lg:w-64',
      p.showFilters ? 'fixed inset-0 z-[60] overflow-y-auto bg-stone-50 p-5' : 'hidden'
    )}>
      <div className="mb-5 flex items-center justify-between lg:hidden">
        <span className="font-bold">Filters</span>
        <button onClick={p.onClose} aria-label="Close filters" className="rounded-lg p-2 hover:bg-stone-200"><FiX /></button>
      </div>

      <div className="space-y-7 lg:sticky lg:top-28">
        {p.hasFilters && (
          <button onClick={p.clearFilters} className="text-sm font-semibold text-primary-600 hover:text-primary-700">
            Clear all filters
          </button>
        )}

        <fieldset>
          <legend className="mb-3 text-xs font-bold uppercase tracking-widest text-stone-500">Category</legend>
          <div className="space-y-1">
            <RadioRow label="All categories" checked={!p.selectedCategory} onChange={() => p.setSelectedCategory('')} />
            {p.categories.map(cat => (
              <RadioRow key={cat.id} label={cat.name} checked={p.selectedCategory === cat.slug}
                onChange={() => p.setSelectedCategory(p.selectedCategory === cat.slug ? '' : cat.slug)} />
            ))}
          </div>
        </fieldset>

        <fieldset>
          <legend className="mb-3 text-xs font-bold uppercase tracking-widest text-stone-500">Brand</legend>
          <div className="space-y-1">
            <RadioRow label="All brands" checked={!p.selectedBrand} onChange={() => p.setSelectedBrand('')} />
            {p.brands.map(brand => (
              <RadioRow key={brand.id} label={brand.name} checked={p.selectedBrand === brand.slug}
                onChange={() => p.setSelectedBrand(p.selectedBrand === brand.slug ? '' : brand.slug)} />
            ))}
          </div>
        </fieldset>

        <fieldset>
          <legend className="mb-3 text-xs font-bold uppercase tracking-widest text-stone-500">Price range</legend>
          <div className="flex items-center gap-2">
            <input type="number" value={p.minPrice} onChange={e => p.setMinPrice(e.target.value)} placeholder="Min"
              className="w-full rounded-lg border border-stone-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100" />
            <span className="text-stone-400">-</span>
            <input type="number" value={p.maxPrice} onChange={e => p.setMaxPrice(e.target.value)} placeholder="Max"
              className="w-full rounded-lg border border-stone-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100" />
          </div>
        </fieldset>

        <fieldset className="space-y-2.5">
          <legend className="mb-3 text-xs font-bold uppercase tracking-widest text-stone-500">Availability</legend>
          <CheckRow label="In stock only" checked={p.inStockOnly} onChange={p.setInStockOnly} />
          <CheckRow label="On sale" checked={p.onSaleOnly} onChange={p.setOnSaleOnly} />
        </fieldset>
      </div>

      {p.showFilters && (
        <button onClick={p.onClose}
          className="mt-8 w-full rounded-xl bg-primary-600 py-3 font-semibold text-white hover:bg-primary-700">
          Show {p.hasFilters ? 'filtered' : ''} results
        </button>
      )}
    </aside>
  );
}

function RadioRow({ label, checked, onChange }: { label: string; checked: boolean; onChange: () => void }) {
  return (
    <label className="flex cursor-pointer items-center gap-2.5 rounded-lg px-2 py-1.5 text-sm hover:bg-stone-100">
      <input type="radio" checked={checked} onChange={onChange} className="h-4 w-4 accent-primary-600" />
      {label}
    </label>
  );
}

function CheckRow({ label, checked, onChange }: { label: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <label className="flex cursor-pointer items-center gap-2.5 text-sm">
      <input type="checkbox" checked={checked} onChange={e => onChange(e.target.checked)} className="h-4 w-4 rounded accent-primary-600" />
      {label}
    </label>
  );
}

export default function ProductsPage() {
  return (
    <Suspense fallback={<div className="mx-auto max-w-7xl px-4 py-16 text-center text-stone-500">Loading products...</div>}>
      <ProductsContent />
    </Suspense>
  );
}
