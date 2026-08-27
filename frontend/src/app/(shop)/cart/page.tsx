'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useAuth } from '@/contexts/AuthContext';
import { cart as cartApi, products as productsApi } from '@/services/api';
import { config } from '@/lib/config';
import { formatKES } from '@/lib/utils';
import Reveal from '@/components/ui/Reveal';
import ProductCard from '@/components/product/ProductCard';
import { FiTrash2, FiMinus, FiPlus, FiShoppingBag, FiArrowRight, FiTruck, FiLoader } from 'react-icons/fi';
import toast from 'react-hot-toast';

export default function CartPage() {
  const { cart, cartLoading, refreshCart, addToCart } = useAuth();
  const [updating, setUpdating] = useState<string | null>(null);
  const [recommendations, setRecommendations] = useState<any[]>([]);

  useEffect(() => {
    const firstSlug = cart?.items?.[0]?.productSlug;
    if (!firstSlug) { setRecommendations([]); return; }
    productsApi.getSimilar(firstSlug)
      .then(res => setRecommendations((res.data || []).slice(0, 4)))
      .catch(() => setRecommendations([]));
  }, [cart?.items?.length]);

  const updateQuantity = async (itemId: string, quantity: number) => {
    if (quantity < 1) return;
    setUpdating(itemId);
    try { await cartApi.updateItem(itemId, quantity); await refreshCart(); }
    catch (e: any) { toast.error(e.message); }
    setUpdating(null);
  };

  const removeItem = async (itemId: string) => {
    setUpdating(itemId);
    try { await cartApi.removeItem(itemId); await refreshCart(); toast.success('Item removed'); }
    catch (e: any) { toast.error(e.message); }
    setUpdating(null);
  };

  if (cartLoading) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-24 text-center">
        <FiLoader className="mx-auto h-8 w-8 animate-spin text-primary-600" />
        <p className="mt-4 text-stone-500">Loading your cart...</p>
      </div>
    );
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-24 text-center">
        <span className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-stone-100">
          <FiShoppingBag className="h-9 w-9 text-stone-400" />
        </span>
        <h1 className="mt-6 text-2xl font-bold">Your cart is empty</h1>
        <p className="mx-auto mt-2 max-w-[42ch] text-stone-500">
          Browse the catalogue and add a few things you like - they will wait for you here.
        </p>
        <Link href="/products" className="mt-8 inline-flex items-center gap-2 rounded-xl bg-primary-600 px-7 py-3.5 font-semibold text-white shadow-lg shadow-primary-600/25 transition-all hover:-translate-y-0.5 hover:bg-primary-700">
          Start shopping <FiArrowRight />
        </Link>
      </div>
    );
  }

  const subtotal = cart.items.reduce((sum, item) => sum + item.priceSnapshot * item.quantity, 0);
  const shipping = subtotal >= config.commerce.freeShippingThreshold ? 0 : config.commerce.shippingCost;
  const tax = Math.round(subtotal * config.commerce.taxRate);
  const total = subtotal + shipping + tax;
  const progress = Math.min(100, Math.round((subtotal / config.commerce.freeShippingThreshold) * 100));
  const missingForFree = config.commerce.freeShippingThreshold - subtotal;

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="mb-8">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary-600">Your basket</p>
        <h1 className="mt-1.5 text-3xl font-bold tracking-tight sm:text-4xl">Shopping cart</h1>
      </div>

      {!shipping && (
        <Reveal className="mb-6">
          <p className="flex items-center gap-2 rounded-xl bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-800 ring-1 ring-emerald-200">
            <FiTruck className="h-4 w-4 shrink-0" /> You have unlocked free delivery.
          </p>
        </Reveal>
      )}

      <div className="grid grid-cols-1 gap-10 lg:grid-cols-[1.6fr_1fr]">
        <div className="space-y-4">
          {cart.items.map((item, i) => (
            <Reveal key={item.id} delay={i * 60}>
              <article className={`flex gap-4 rounded-2xl border border-stone-200/80 bg-white p-4 transition-opacity ${updating === item.id ? 'opacity-50' : ''}`}>
                <Link href={`/products/${item.productSlug}`} className="shrink-0 overflow-hidden rounded-xl bg-stone-100">
                  {item.productImage ? (
                    <img src={item.productImage} alt={item.productName} className="h-24 w-24 object-cover sm:h-28 sm:w-28" />
                  ) : (
                    <div className="flex h-24 w-24 items-center justify-center text-stone-400"><FiShoppingBag /></div>
                  )}
                </Link>

                <div className="flex min-w-0 flex-1 flex-col">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <Link href={`/products/${item.productSlug}`} className="line-clamp-2 font-semibold hover:text-primary-700">
                        {item.productName}
                      </Link>
                      <p className="mt-0.5 text-sm text-stone-500">{formatKES(item.priceSnapshot)} each</p>
                    </div>
                    <button onClick={() => removeItem(item.id)} disabled={updating === item.id}
                      className="rounded-lg p-2 text-stone-400 transition-colors hover:bg-rose-50 hover:text-rose-600" aria-label={`Remove ${item.productName}`}>
                      <FiTrash2 />
                    </button>
                  </div>

                  <div className="mt-auto flex items-center justify-between pt-3">
                    <div className="inline-flex items-center rounded-lg border border-stone-300">
                      <button onClick={() => updateQuantity(item.id, item.quantity - 1)} disabled={updating === item.id}
                        className="px-2.5 py-1.5 text-stone-600 hover:text-stone-900 disabled:opacity-40" aria-label="Decrease quantity"><FiMinus /></button>
                      <span className="w-9 text-center text-sm font-semibold">{item.quantity}</span>
                      <button
                        onClick={async () => { setUpdating(item.id); try { await addToCart(item.productId, 1); } catch (e: any) { toast.error(e.message); } setUpdating(null); }}
                        disabled={updating === item.id}
                        className="px-2.5 py-1.5 text-stone-600 hover:text-stone-900 disabled:opacity-40" aria-label="Increase quantity"><FiPlus /></button>
                    </div>
                    <p className="font-bold">{formatKES(item.lineTotal)}</p>
                  </div>
                </div>
              </article>
            </Reveal>
          ))}
        </div>

        <aside className="lg:sticky lg:top-28 lg:self-start">
          <Reveal delay={120}>
            <div className="rounded-2xl border border-stone-200/80 bg-white p-6">
              <h2 className="text-lg font-bold">Order summary</h2>

              <div className="mt-5 rounded-xl bg-stone-50 p-4">
                {shipping > 0 && missingForFree > 0 ? (
                  <>
                    <p className="text-sm text-stone-600">
                      Add <strong className="text-stone-900">{formatKES(missingForFree)}</strong> more for free delivery
                    </p>
                    <div className="mt-2.5 h-2 overflow-hidden rounded-full bg-stone-200" role="progressbar" aria-valuenow={progress} aria-valuemin={0} aria-valuemax={100}>
                      <div className="h-full rounded-full bg-gradient-to-r from-primary-500 to-emerald-400 transition-all duration-700" style={{ width: `${progress}%` }} />
                    </div>
                  </>
                ) : (
                  <p className="flex items-center gap-2 text-sm font-medium text-emerald-700">
                    <FiTruck className="h-4 w-4" /> Free delivery unlocked
                  </p>
                )}
              </div>

              <dl className="mt-5 space-y-3 text-sm">
                <div className="flex justify-between"><dt className="text-stone-500">Subtotal</dt><dd className="font-semibold">{formatKES(subtotal)}</dd></div>
                <div className="flex justify-between"><dt className="text-stone-500">Delivery</dt><dd className="font-semibold">{shipping === 0 ? 'Free' : formatKES(shipping)}</dd></div>
                <div className="flex justify-between"><dt className="text-stone-500">VAT ({Math.round(config.commerce.taxRate * 100)}%)</dt><dd className="font-semibold">{formatKES(tax)}</dd></div>
                <div className="border-t border-stone-200 pt-3">
                  <div className="flex justify-between text-base">
                    <dt className="font-bold">Total</dt><dd className="text-xl font-extrabold">{formatKES(total)}</dd>
                  </div>
                </div>
              </dl>

              <Link href="/checkout" className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-primary-600 py-3.5 font-semibold text-white shadow-lg shadow-primary-600/25 transition-all hover:-translate-y-0.5 hover:bg-primary-700">
                Proceed to checkout <FiArrowRight />
              </Link>
              <Link href="/products" className="mt-3 block text-center text-sm font-medium text-stone-500 hover:text-primary-600">
                Continue shopping
              </Link>
            </div>
          </Reveal>
        </aside>
      </div>

      {recommendations.length > 0 && (
        <section className="mt-16">
          <Reveal>
            <h2 className="text-2xl font-bold tracking-tight">Pairs well with your picks</h2>
            <p className="mt-1 text-sm text-stone-500">Based on what is already in your cart.</p>
          </Reveal>
          <div className="mt-6 grid grid-cols-2 gap-5 lg:grid-cols-4">
            {recommendations.map((p, i) => (
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
