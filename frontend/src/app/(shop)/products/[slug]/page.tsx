'use client';
import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'next/navigation';
import Link from 'next/link';
import { products as productsApi } from '@/services/api';
import { config } from '@/lib/config';
import { Product, Review } from '@/types';
import { useAuth } from '@/contexts/AuthContext';
import { formatKES, formatDate } from '@/lib/utils';
import Reveal from '@/components/ui/Reveal';
import ProductCard from '@/components/product/ProductCard';
import {
  FiStar,
  FiShoppingBag,
  FiMinus,
  FiPlus,
  FiChevronRight,
  FiCheck,
  FiTruck,
  FiRotateCcw,
  FiShield,
  FiChevronDown,
} from 'react-icons/fi';
import toast from 'react-hot-toast';

export default function ProductDetailPage() {
  const { slug } = useParams();
  const { addToCart } = useAuth();
  const [product, setProduct] = useState<Product | null>(null);
  const [similar, setSimilar] = useState<Product[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [selectedImage, setSelectedImage] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [adding, setAdding] = useState(false);
  const [added, setAdded] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setSelectedImage(0);
    setQuantity(1);
    setLoading(true);
    productsApi.getBySlug(slug as string).then(res => {
      setProduct(res.data || null);
      if (res.data) {
        productsApi.getSimilar(res.data.slug).then(r => setSimilar(r.data || [])).catch(() => {});
        productsApi.getReviews(res.data.slug).then(r => setReviews((r.data as any)?.reviews || [])).catch(() => {});
      }
    }).finally(() => setLoading(false));
  }, [slug]);

  const handleAdd = useCallback(async () => {
    if (!product || adding) return;
    setAdding(true);
    try {
      await addToCart(product.id, quantity);
      toast.success(`${quantity} x ${product.name} added to cart`);
      setAdded(true);
      setTimeout(() => setAdded(false), 2000);
    } catch (e: any) {
      toast.error(e.message || 'Failed to add to cart');
    }
    setAdding(false);
  }, [product, quantity, adding, addToCart]);

  if (loading) return <DetailSkeleton />;

  if (!product) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-24 text-center">
        <p className="text-xl font-semibold text-stone-800">We could not find that product</p>
        <p className="mt-2 text-stone-500">It may have sold out or the link is outdated.</p>
        <Link href="/products" className="mt-6 inline-block rounded-xl bg-primary-600 px-6 py-3 font-semibold text-white hover:bg-primary-700">
          Browse all products
        </Link>
      </div>
    );
  }

  const image = product.images?.[selectedImage];
  const lowStock = product.inStock && product.stock <= 5;

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <nav className="flex flex-wrap items-center gap-1.5 text-sm text-stone-500" aria-label="Breadcrumb">
        <Link href="/" className="hover:text-primary-600">Home</Link>
        <FiChevronRight className="h-3.5 w-3.5 text-stone-300" />
        <Link href="/products" className="hover:text-primary-600">Products</Link>
        {product.category && (
          <>
            <FiChevronRight className="h-3.5 w-3.5 text-stone-300" />
            <Link href={`/products?category=${product.category.slug}`} className="hover:text-primary-600">{product.category.name}</Link>
          </>
        )}
        <FiChevronRight className="h-3.5 w-3.5 text-stone-300" />
        <span className="text-stone-900">{product.name}</span>
      </nav>

      <div className="mt-8 grid grid-cols-1 gap-12 lg:grid-cols-[1.05fr_0.95fr]">
        <Gallery product={product} selected={selectedImage} onSelect={setSelectedImage} />

        <div className="lg:sticky lg:top-28 lg:self-start">
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary-600">
            {product.brand?.name || 'Marketplace'}
          </p>
          <h1 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">{product.name}</h1>

          <div className="mt-4 flex items-center gap-3">
            <div className="flex items-center gap-0.5">
              {[1, 2, 3, 4, 5].map(s => (
                <FiStar key={s} className={`h-4 w-4 ${s <= Math.round(product.averageRating || 0) ? 'fill-amber-400 text-amber-400' : 'text-stone-300'}`} />
              ))}
            </div>
            <span className="text-sm font-medium">{product.averageRating ? product.averageRating.toFixed(1) : 'New'}</span>
            {product.reviewCount > 0 && <span className="text-sm text-stone-500">({product.reviewCount} reviews)</span>}
            <StockPill inStock={product.inStock} stock={product.stock} />
          </div>

          <div className="mt-6 flex items-baseline gap-3">
            <span className="text-4xl font-extrabold tracking-tight">{formatKES(product.price)}</span>
            {product.compareAtPrice && (
              <>
                <span className="text-xl text-stone-400 line-through">{formatKES(product.compareAtPrice)}</span>
                <span className="rounded-full bg-rose-100 px-2.5 py-1 text-sm font-semibold text-rose-700">
                  Save {product.discountPercentage}%
                </span>
              </>
            )}
          </div>

          <p className="mt-6 max-w-[60ch] leading-relaxed text-stone-600">{product.description}</p>

          {product.inStock ? (
            <>
              {lowStock && (
                <p className="mt-5 inline-flex items-center gap-2 rounded-full bg-amber-50 px-4 py-2 text-sm font-medium text-amber-800 ring-1 ring-amber-200">
                  Only {product.stock} left in stock
                </p>
              )}
              <div className="mt-7 flex items-stretch gap-3">
                <div className="flex items-center rounded-xl border border-stone-300">
                  <button onClick={() => setQuantity(q => Math.max(1, q - 1))} className="px-3.5 py-3 text-stone-600 hover:text-stone-900 disabled:opacity-40" disabled={quantity <= 1} aria-label="Decrease quantity">
                    <FiMinus />
                  </button>
                  <span className="w-10 text-center font-semibold">{quantity}</span>
                  <button onClick={() => setQuantity(q => Math.min(product.stock, q + 1))} className="px-3.5 py-3 text-stone-600 hover:text-stone-900 disabled:opacity-40" disabled={quantity >= product.stock} aria-label="Increase quantity">
                    <FiPlus />
                  </button>
                </div>
                <button
                  onClick={handleAdd}
                  disabled={adding}
                  className={`flex flex-1 items-center justify-center gap-2 rounded-xl py-3.5 text-base font-semibold text-white shadow-lg transition-all active:scale-[0.99] ${
                    added ? 'bg-emerald-600 shadow-emerald-600/25' : 'bg-primary-600 shadow-primary-600/25 hover:bg-primary-700 disabled:opacity-60'
                  }`}
                >
                  {added ? <FiCheck className="h-5 w-5" /> : <FiShoppingBag className="h-5 w-5" />}
                  {added ? 'Added to cart' : adding ? 'Adding...' : 'Add to cart'}
                </button>
              </div>
            </>
          ) : (
            <p className="mt-7 rounded-xl bg-stone-100 px-5 py-4 text-center font-medium text-stone-600">
              Out of stock - check back soon
            </p>
          )}

          <Perks />

          <SpecsAccordion product={product} />
        </div>
      </div>

      {reviews.length > 0 && (
        <Reveal className="mt-20">
          <h2 className="text-2xl font-bold tracking-tight">Customer reviews</h2>
          <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {reviews.slice(0, 6).map(review => (
              <article key={review.id} className="rounded-2xl border border-stone-200/80 bg-white p-5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-0.5">
                    {[1, 2, 3, 4, 5].map(s => (
                      <FiStar key={s} className={`h-3.5 w-3.5 ${s <= review.rating ? 'fill-amber-400 text-amber-400' : 'text-stone-300'}`} />
                    ))}
                  </div>
                  <span className="text-xs text-stone-400">{formatDate(review.createdAt)}</span>
                </div>
                {review.title && <h3 className="mt-3 font-semibold">{review.title}</h3>}
                <p className="mt-1.5 line-clamp-4 text-sm leading-relaxed text-stone-600">{review.content}</p>
              </article>
            ))}
          </div>
        </Reveal>
      )}

      {similar.length > 0 && (
        <section className="mt-20">
          <Reveal>
            <h2 className="text-2xl font-bold tracking-tight">You may also like</h2>
          </Reveal>
          <div className="mt-6 grid grid-cols-2 gap-5 lg:grid-cols-4">
            {similar.slice(0, 4).map((p, i) => (
              <Reveal key={p.id} delay={i * 80}>
                <ProductCard product={p} />
              </Reveal>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

function Gallery({ product, selected, onSelect }: { product: Product; selected: number; onSelect: (i: number) => void }) {
  const images = product.images || [];
  return (
    <div>
      <div className="relative aspect-square overflow-hidden rounded-3xl border border-stone-200/80 bg-stone-100">
        {images[selected] ? (
          <img src={images[selected].url} alt={images[selected].alt || product.name} className="h-full w-full object-cover" />
        ) : (
          <div className="flex h-full w-full items-center justify-center text-stone-400">No image</div>
        )}
        {product.onSale && product.discountPercentage > 0 && (
          <span className="absolute left-4 top-4 rounded-full bg-rose-600 px-3 py-1.5 text-xs font-semibold text-white shadow-md">
            -{product.discountPercentage}% today
          </span>
        )}
      </div>
      {images.length > 1 && (
        <div className="mt-4 grid grid-cols-5 gap-3">
          {images.map((img, i) => (
            <button
              key={img.id}
              onClick={() => onSelect(i)}
              aria-label={`View image ${i + 1}`}
              className={`aspect-square overflow-hidden rounded-xl border-2 transition-all ${
                i === selected ? 'border-primary-600 ring-2 ring-primary-100' : 'border-transparent opacity-70 hover:opacity-100'
              }`}
            >
              <img src={img.url} alt={img.alt || ''} className="h-full w-full object-cover" />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function StockPill({ inStock, stock }: { inStock: boolean; stock: number }) {
  if (!inStock) {
    return <span className="rounded-full bg-stone-200 px-3 py-1 text-xs font-semibold text-stone-600">Sold out</span>;
  }
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700 ring-1 ring-emerald-200">
      <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
      In stock
    </span>
  );
}

function Perks() {
  const perks = [
    { icon: FiTruck, title: 'Fast delivery', note: `Free over ${formatKES(config.commerce.freeShippingThreshold)}` },
    { icon: FiRotateCcw, title: 'Easy returns', note: '14 days, no questions' },
    { icon: FiShield, title: 'Secure payment', note: 'M-Pesa, card and PayPal' },
  ];
  return (
    <div className="mt-8 grid grid-cols-3 gap-3 rounded-2xl border border-stone-200/80 bg-white p-4">
      {perks.map(perk => (
        <div key={perk.title} className="flex flex-col items-center gap-1.5 text-center">
          <perk.icon className="h-5 w-5 text-primary-600" />
          <p className="text-xs font-semibold">{perk.title}</p>
          <p className="text-[11px] leading-tight text-stone-500">{perk.note}</p>
        </div>
      ))}
    </div>
  );
}

function SpecsAccordion({ product }: { product: Product }) {
  const specs = [
    { label: 'SKU', value: product.sku },
    { label: 'Category', value: product.category?.name },
    { label: 'Brand', value: product.brand?.name },
    ...(product.weight > 0 ? [{ label: `Weight`, value: `${product.weight} ${product.weightUnit || 'kg'}` }] : []),
    ...(product.dimensions ? [{ label: 'Dimensions', value: typeof product.dimensions === 'string' ? product.dimensions : JSON.stringify(product.dimensions) }] : []),
  ].filter(spec => spec.value);

  if (specs.length === 0) return null;

  return (
    <details className="faq mt-6 rounded-2xl border border-stone-200/80 bg-white px-5 open:pb-2 [&_summary::-webkit-details-marker]:hidden">
      <summary className="flex cursor-pointer list-none items-center justify-between py-4 font-semibold">
        Product details
        <FiChevronDown className="faq-icon h-4 w-4 text-stone-500" />
      </summary>
      <dl className="space-y-2.5 border-t border-stone-100 pt-4 pb-3 text-sm">
        {specs.map(spec => (
          <div key={spec.label} className="flex justify-between gap-6">
            <dt className="text-stone-500">{spec.label}</dt>
            <dd className="text-right font-medium">{spec.value}</dd>
          </div>
        ))}
      </dl>
    </details>
  );
}

function DetailSkeleton() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="h-4 w-64 animate-pulse rounded bg-stone-200/70" />
      <div className="mt-8 grid grid-cols-1 gap-12 lg:grid-cols-2">
        <div className="aspect-square animate-pulse rounded-3xl bg-stone-200/70" />
        <div className="space-y-5">
          <div className="h-3 w-24 animate-pulse rounded bg-stone-200/70" />
          <div className="h-9 w-3/4 animate-pulse rounded bg-stone-200/70" />
          <div className="h-4 w-40 animate-pulse rounded bg-stone-200/70" />
          <div className="h-10 w-44 animate-pulse rounded bg-stone-200/70" />
          <div className="h-24 w-full animate-pulse rounded bg-stone-200/70" />
          <div className="h-14 w-full animate-pulse rounded-xl bg-stone-200/70" />
        </div>
      </div>
    </div>
  );
}
