'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { products as productsApi, categories as categoriesApi } from '@/services/api';
import { config } from '@/lib/config';
import { Product, Category } from '@/types';
import { formatKES } from '@/lib/utils';
import { FiArrowRight, FiStar } from 'react-icons/fi';

export default function HomePage() {
  const [featured, setFeatured] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      productsApi.search({ page: '0', size: String(config.pages.featuredProducts), sortBy: 'newest' }),
      categoriesApi.list(),
    ]).then(([prods, cats]) => {
      setFeatured(prods.data?.products || []);
      setCategories(cats.data || []);
    }).finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <section className="bg-gradient-to-r from-primary-600 to-primary-800 text-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
          <h1 className="text-4xl md:text-5xl font-bold mb-4">Welcome to i-love-shopping</h1>
          <p className="text-xl text-primary-100 mb-8 max-w-2xl">Discover quality products at great prices. Your one-stop shop for everything you need.</p>
          <Link href="/products" className="inline-flex items-center gap-2 bg-white text-primary-700 px-6 py-3 rounded-lg font-semibold hover:bg-primary-50 transition">
            Shop Now <FiArrowRight />
          </Link>
        </div>
      </section>

      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <h2 className="text-2xl font-bold mb-6">Shop by Category</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {categories.map(cat => (
            <Link key={cat.id} href={`/products?category=${cat.slug}`}
              className="bg-white rounded-lg p-6 text-center hover:shadow-md transition border">
              <p className="font-semibold">{cat.name}</p>
              {cat.productCount !== undefined && <p className="text-sm text-gray-500 mt-1">{cat.productCount} products</p>}
            </Link>
          ))}
        </div>
      </section>

      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold">Featured Products</h2>
          <Link href="/products" className="text-primary-600 hover:text-primary-700 flex items-center gap-1 text-sm">View All <FiArrowRight /></Link>
        </div>
        {loading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {[...Array(8)].map((_, i) => <div key={i} className="bg-white rounded-lg h-72 animate-pulse" />)}
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {featured.map(product => (
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
                </div>
                <div className="p-4">
                  <p className="text-xs text-gray-500">{product.brand?.name}</p>
                  <h3 className="font-semibold text-sm mt-1 line-clamp-2">{product.name}</h3>
                  <div className="flex items-center gap-1 mt-1">
                    <FiStar className="h-3 w-3 fill-yellow-400 text-yellow-400" />
                    <span className="text-xs text-gray-600">{product.averageRating?.toFixed(1) || 'New'}</span>
                  </div>
                  <div className="mt-2 flex items-baseline gap-2">
                    <span className="font-bold text-primary-600">{formatKES(product.price)}</span>
                    {product.compareAtPrice && <span className="text-sm text-gray-400 line-through">{formatKES(product.compareAtPrice)}</span>}
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
