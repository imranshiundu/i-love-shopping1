'use client';
import Link from 'next/link';
import { useState } from 'react';
import { Product } from '@/types';
import { formatKES, cn } from '@/lib/utils';
import { useAuth } from '@/contexts/AuthContext';
import { FiStar, FiShoppingBag, FiCheck, FiEye } from 'react-icons/fi';
import toast from 'react-hot-toast';

export default function ProductCard({ product }: { product: Product }) {
  const { addToCart } = useAuth();
  const [adding, setAdding] = useState(false);
  const [added, setAdded] = useState(false);
  const image = product.images?.[0];

  const handleQuickAdd = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (!product.inStock || adding) return;
    setAdding(true);
    try {
      await addToCart(product.id, 1);
      setAdded(true);
      toast.success(`${product.name} added to cart`);
      setTimeout(() => setAdded(false), 1800);
    } catch (err: any) {
      toast.error(err.message || 'Could not add to cart');
    }
    setAdding(false);
  };

  return (
    <Link
      href={`/products/${product.slug}`}
      className="group card-lift flex flex-col overflow-hidden rounded-2xl border border-stone-200/80 bg-white"
    >
      <div className="relative aspect-[4/5] overflow-hidden bg-stone-100">
        {image ? (
          <img
            src={image.url}
            alt={image.alt || product.name}
            loading="lazy"
            className="img-zoom h-full w-full object-cover"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center text-sm text-stone-400">
            No image
          </div>
        )}

        <div className="absolute left-3 top-3 flex flex-col gap-1.5">
          {product.onSale && product.discountPercentage > 0 && (
            <span className="rounded-full bg-rose-600 px-2.5 py-1 text-[11px] font-semibold tracking-wide text-white shadow-sm">
              -{product.discountPercentage}%
            </span>
          )}
          {!product.inStock && (
            <span className="rounded-full bg-stone-900/85 px-2.5 py-1 text-[11px] font-medium tracking-wide text-white">
              Sold out
            </span>
          )}
        </div>

        <div className="quick-add absolute inset-x-3 bottom-3">
          <button
            onClick={handleQuickAdd}
            disabled={!product.inStock || adding}
            className={cn(
              'flex w-full items-center justify-center gap-2 rounded-xl py-2.5 text-sm font-semibold shadow-lg backdrop-blur transition-colors',
              added
                ? 'bg-emerald-600 text-white'
                : 'bg-white/95 text-stone-900 hover:bg-primary-600 hover:text-white disabled:cursor-not-allowed disabled:opacity-60'
            )}
          >
            {added ? <FiCheck className="h-4 w-4" /> : <FiShoppingBag className="h-4 w-4" />}
            {added ? 'Added' : adding ? 'Adding...' : 'Quick add'}
          </button>
        </div>

        <span className="pointer-events-none absolute right-3 top-3 rounded-full bg-white/90 p-2 text-stone-700 opacity-0 shadow-sm backdrop-blur transition-opacity duration-300 group-hover:opacity-100">
          <FiEye className="h-4 w-4" />
        </span>
      </div>

      <div className="flex flex-1 flex-col gap-1 p-4">
        <p className="text-[11px] font-medium uppercase tracking-[0.14em] text-stone-400">
          {product.brand?.name || 'Marketplace'}
        </p>
        <h3 className="line-clamp-1 text-sm font-semibold text-stone-900">{product.name}</h3>
        <div className="mt-auto flex items-center justify-between pt-2">
          <div className="flex items-baseline gap-1.5">
            <span className="text-base font-bold text-stone-900">{formatKES(product.price)}</span>
            {product.compareAtPrice && (
              <span className="text-xs text-stone-400 line-through">{formatKES(product.compareAtPrice)}</span>
            )}
          </div>
          <div className="flex items-center gap-1 text-xs text-stone-500">
            <FiStar className="h-3.5 w-3.5 fill-amber-400 text-amber-400" />
            <span>{product.averageRating ? product.averageRating.toFixed(1) : 'New'}</span>
            {product.reviewCount > 0 && <span className="text-stone-300">({product.reviewCount})</span>}
          </div>
        </div>
      </div>
    </Link>
  );
}
