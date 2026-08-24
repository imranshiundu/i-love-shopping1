'use client';
import { useState, useEffect, useCallback, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { products as productsApi, categories as categoriesApi, brands as brandsApi } from '@/services/api';
import { config } from '@/lib/config';
import { Product, Category, Brand } from '@/types';
import { formatKES, cn } from '@/lib/utils';
import { FiStar, FiFilter, FiX, FiChevronLeft, FiChevronRight } from 'react-icons/fi';

function ProductsContent() {
  const searchParams = useSearchParams();
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);
  const [loading, setLoading] = useState(true);
  const [pagination, setPagination] = useState({ page: 0, size: 20, totalElements: 0, totalPages: 0 });
  const [showFilters, setShowFilters] = useState(false);

  const [query, setQuery] = useState(searchParams.get('q') || '');
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

  const handleSearch = (e: React.FormEvent) => { e.preventDefault(); setCurrentPage(0); fetchProducts(); };

  const clearFilters = () => { setQuery(''); setSelectedCategory(''); setSelectedBrand(''); setMinPrice(''); setMaxPrice(''); setInStockOnly(false); setOnSaleOnly(false); setCurrentPage(0); };

  const hasFilters = query || selectedCategory || selectedBrand || minPrice || maxPrice || inStockOnly || onSaleOnly;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex flex-col md:flex-row gap-8">
        <aside className={cn("md:w-64 flex-shrink-0", showFilters ? "block" : "hidden md:block")}>
          <div className="bg-white rounded-lg p-4 border space-y-6">
            <div className="flex items-center justify-between">
              <h3 className="font-semibold">Filters</h3>
              {hasFilters && <button onClick={clearFilters} className="text-sm text-primary-600 hover:underline">Clear all</button>}
            </div>

            <div>
              <h4 className="text-sm font-medium mb-2">Categories</h4>
              {categories.map(cat => (
                <label key={cat.id} className="flex items-center gap-2 text-sm cursor-pointer py-1">
                  <input type="radio" name="category" checked={selectedCategory === cat.slug}
                    onChange={() => { setSelectedCategory(selectedCategory === cat.slug ? '' : cat.slug); setCurrentPage(0); }} />
                  {cat.name}
                </label>
              ))}
            </div>

            <div>
              <h4 className="text-sm font-medium mb-2">Brands</h4>
              {brands.map(brand => (
                <label key={brand.id} className="flex items-center gap-2 text-sm cursor-pointer py-1">
                  <input type="radio" name="brand" checked={selectedBrand === brand.slug}
                    onChange={() => { setSelectedBrand(selectedBrand === brand.slug ? '' : brand.slug); setCurrentPage(0); }} />
                  {brand.name}
                </label>
              ))}
            </div>

            <div>
              <h4 className="text-sm font-medium mb-2">Price Range (KES)</h4>
              <div className="flex gap-2">
                <input type="number" value={minPrice} onChange={e => setMinPrice(e.target.value)} placeholder="Min" className="w-full border rounded px-2 py-1 text-sm" />
                <input type="number" value={maxPrice} onChange={e => setMaxPrice(e.target.value)} placeholder="Max" className="w-full border rounded px-2 py-1 text-sm" />
              </div>
            </div>

            <div className="space-y-2">
              <label className="flex items-center gap-2 text-sm cursor-pointer">
                <input type="checkbox" checked={inStockOnly} onChange={e => { setInStockOnly(e.target.checked); setCurrentPage(0); }} />
                In Stock Only
              </label>
              <label className="flex items-center gap-2 text-sm cursor-pointer">
                <input type="checkbox" checked={onSaleOnly} onChange={e => { setOnSaleOnly(e.target.checked); setCurrentPage(0); }} />
                On Sale
              </label>
            </div>
          </div>
        </aside>

        <div className="flex-1">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
            <div>
              <h1 className="text-2xl font-bold">{query ? `Results for "${query}"` : 'All Products'}</h1>
              <p className="text-sm text-gray-500">{pagination.totalElements} products found</p>
            </div>
            <div className="flex items-center gap-3">
              <button onClick={() => setShowFilters(!showFilters)} className="md:hidden flex items-center gap-1 text-sm border rounded px-3 py-2"><FiFilter /> Filters</button>
              <select value={sortBy} onChange={e => { setSortBy(e.target.value); setCurrentPage(0); }}
                className="border rounded px-3 py-2 text-sm">
                <option value="relevance">Relevance</option>
                <option value="price_asc">Price: Low to High</option>
                <option value="price_desc">Price: High to Low</option>
                <option value="newest">Newest</option>
                <option value="rating">Highest Rated</option>
              </select>
            </div>
          </div>

          {loading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {[...Array(12)].map((_, i) => <div key={i} className="bg-white rounded-lg h-72 animate-pulse" />)}
            </div>
          ) : products.length === 0 ? (
            <div className="text-center py-16">
              <p className="text-gray-500 text-lg">No products found</p>
              <button onClick={clearFilters} className="mt-4 text-primary-600 hover:underline">Clear filters</button>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {products.map(product => (
                  <Link key={product.id} href={`/products/${product.slug}`}
                    className="bg-white rounded-lg overflow-hidden hover:shadow-lg transition group border">
                    <div className="aspect-square bg-gray-100 relative overflow-hidden">
                      {product.images?.[0] ? (
                        <img src={product.images[0].url} alt={product.images[0].alt || product.name}
                          className="w-full h-full object-cover group-hover:scale-105 transition" />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-gray-400">No Image</div>
                      )}
                      {product.onSale && <span className="absolute top-2 left-2 bg-red-500 text-white text-xs px-2 py-1 rounded">-{product.discountPercentage}%</span>}
                      {!product.inStock && <span className="absolute top-2 right-2 bg-gray-800 text-white text-xs px-2 py-1 rounded">Out of Stock</span>}
                    </div>
                    <div className="p-4">
                      <p className="text-xs text-gray-500">{product.brand?.name}</p>
                      <h3 className="font-semibold text-sm mt-1 line-clamp-2">{product.name}</h3>
                      <div className="flex items-center gap-1 mt-1">
                        <FiStar className="h-3 w-3 fill-yellow-400 text-yellow-400" />
                        <span className="text-xs text-gray-600">{product.averageRating?.toFixed(1) || 'New'}</span>
                        {product.reviewCount > 0 && <span className="text-xs text-gray-400">({product.reviewCount})</span>}
                      </div>
                      <div className="mt-2 flex items-baseline gap-2">
                        <span className="font-bold text-primary-600">{formatKES(product.price)}</span>
                        {product.compareAtPrice && <span className="text-sm text-gray-400 line-through">{formatKES(product.compareAtPrice)}</span>}
                      </div>
                    </div>
                  </Link>
                ))}
              </div>

              {pagination.totalPages > 1 && (
                <div className="flex items-center justify-center gap-2 mt-8">
                  <button onClick={() => setCurrentPage(p => Math.max(0, p - 1))} disabled={currentPage === 0}
                    className="p-2 rounded border disabled:opacity-50"><FiChevronLeft /></button>
                  {Array.from({ length: Math.min(5, pagination.totalPages) }, (_, i) => {
                    const start = Math.max(0, Math.min(currentPage - 2, pagination.totalPages - 5));
                    return start + i;
                  }).filter(p => p < pagination.totalPages).map(p => (
                    <button key={p} onClick={() => setCurrentPage(p)}
                      className={cn("w-10 h-10 rounded border text-sm", p === currentPage ? "bg-primary-600 text-white" : "hover:bg-gray-50")}>{p + 1}</button>
                  ))}
                  <button onClick={() => setCurrentPage(p => Math.min(pagination.totalPages - 1, p + 1))} disabled={currentPage >= pagination.totalPages - 1}
                    className="p-2 rounded border disabled:opacity-50"><FiChevronRight /></button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default function ProductsPage() {
  return (
    <Suspense fallback={<div className="max-w-7xl mx-auto px-4 py-8 text-center">Loading products...</div>}>
      <ProductsContent />
    </Suspense>
  );
}
