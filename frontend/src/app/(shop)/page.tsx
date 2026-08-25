'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { products as productsApi, categories as categoriesApi } from '@/services/api';
import { config } from '@/lib/config';
import { Product, Category } from '@/types';
import { formatKES } from '@/lib/utils';
import Reveal from '@/components/ui/Reveal';
import ProductCard from '@/components/product/ProductCard';
import {
  FiArrowRight,
  FiArrowUpRight,
  FiTruck,
  FiShield,
  FiRefreshCcw,
  FiHeadphones,
} from 'react-icons/fi';

const PERKS = [
  { icon: FiTruck, label: `Free delivery over ${formatKES(config.commerce.freeShippingThreshold)}` },
  { icon: FiShield, label: 'Secure checkout' },
  { icon: FiRefreshCcw, label: '14-day returns' },
  { icon: FiHeadphones, label: 'Support, seven days a week' },
];

export default function HomePage() {
  const [featured, setFeatured] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      productsApi.search({ page: '0', size: String(config.pages.featuredProducts), sortBy: 'newest' }),
      categoriesApi.list(),
    ])
      .then(([prods, cats]) => {
        setFeatured(prods.data?.products || []);
        setCategories(cats.data || []);
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <Hero />

      <PerkTicker />

      <CategoriesSection categories={categories} loading={loading} />

      <FeaturedSection featured={featured} loading={loading} />

      <SaleBanner />
    </div>
  );
}

function Hero() {
  return (
    <section className="relative overflow-hidden bg-stone-950 text-white">
      <div
        aria-hidden
        className="absolute inset-0"
        style={{
          background:
            'radial-gradient(52rem 30rem at 82% -10%, oklch(0.45 0.14 250 / 0.55), transparent 60%),' +
            'radial-gradient(40rem 26rem at -12% 110%, oklch(0.5 0.1 210 / 0.35), transparent 60%),' +
            'linear-gradient(160deg, #101418 0%, #141a22 55%, #101418 100%)',
        }}
      />
      <div
        aria-hidden
        className="absolute inset-0 opacity-[0.05]"
        style={{
          backgroundImage:
            'linear-gradient(to right, white 1px, transparent 1px), linear-gradient(to bottom, white 1px, transparent 1px)',
          backgroundSize: '56px 56px',
          maskImage: 'radial-gradient(60rem 40rem at 70% 20%, black, transparent)',
        }}
      />

      <div className="relative mx-auto grid max-w-7xl grid-cols-1 items-center gap-12 px-4 pb-24 pt-16 sm:px-6 lg:min-h-[88dvh] lg:grid-cols-[1.05fr_0.95fr] lg:gap-8 lg:pb-28 lg:pt-20 xl:px-8">
        <div>
          <p
            className="hero-rise inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/5 px-4 py-1.5 text-xs font-medium uppercase tracking-[0.18em] text-primary-200 backdrop-blur"
            style={{ animationDelay: '80ms' }}
          >
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" />
            New season, new arrivals
          </p>

          <h1
            className="hero-rise mt-6 text-5xl font-extrabold leading-[0.98] tracking-tight sm:text-6xl xl:text-7xl"
            style={{ animationDelay: '180ms' }}
          >
            Everyday things,
            <br />
            <span className="text-primary-300">exceptionally made.</span>
          </h1>

          <p
            className="hero-rise mt-6 max-w-[52ch] text-lg leading-relaxed text-stone-300"
            style={{ animationDelay: '300ms' }}
          >
            Handpicked ceramics, textiles and home goods from independent makers.
            Fair prices, honest materials, delivered across Kenya in days not weeks.
          </p>

          <div className="hero-rise mt-9 flex flex-wrap items-center gap-4" style={{ animationDelay: '420ms' }}>
            <Link
              href="/products"
              className="group inline-flex items-center gap-2 rounded-xl bg-primary-500 px-7 py-3.5 text-base font-semibold text-white shadow-lg shadow-primary-500/25 transition-all hover:-translate-y-0.5 hover:bg-primary-400 hover:shadow-primary-400/30 active:translate-y-0"
            >
              Start shopping
              <FiArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
            </Link>
            <Link
              href="/products?sortBy=rating"
              className="inline-flex items-center gap-2 rounded-xl border border-white/20 px-7 py-3.5 text-base font-semibold text-stone-100 backdrop-blur transition-colors hover:border-white/40 hover:bg-white/5"
            >
              Top rated
            </Link>
          </div>
        </div>

        <div aria-hidden className="relative hidden h-full min-h-[480px] select-none lg:block">
          <div
            className="animate-float absolute right-4 top-2 w-64 overflow-hidden rounded-3xl border border-white/10 shadow-2xl shadow-black/50 xl:w-72"
            style={{ '--float-rot': '3deg', animationDelay: '0s' } as React.CSSProperties}
          >
            <img src="https://picsum.photos/seed/iloveshopping-hero1/600/720" alt="" className="aspect-[5/6] w-full object-cover" />
          </div>
          <div
            className="animate-float absolute left-0 top-40 w-52 overflow-hidden rounded-3xl border border-white/10 shadow-2xl shadow-black/50 xl:w-60"
            style={{ '--float-rot': '-5deg', animationDelay: '1.4s' } as React.CSSProperties}
          >
            <img src="https://picsum.photos/seed/iloveshopping-hero2/520/640" alt="" className="aspect-[5/6] w-full object-cover" />
          </div>
          <div
            className="animate-float absolute bottom-0 right-16 w-44 overflow-hidden rounded-3xl border border-white/10 shadow-2xl shadow-black/50 xl:w-52"
            style={{ '--float-rot': '-2deg', animationDelay: '2.6s' } as React.CSSProperties}
          >
            <img src="https://picsum.photos/seed/iloveshopping-hero3/480/480" alt="" className="aspect-square w-full object-cover" />
          </div>

          <div
            className="hero-rise absolute left-8 top-6 rounded-2xl border border-white/10 bg-white/10 px-4 py-3 backdrop-blur-md"
            style={{ animationDelay: '700ms' }}
          >
            <p className="text-xs uppercase tracking-widest text-stone-300">Free delivery</p>
            <p className="text-sm font-semibold">{`Orders over ${formatKES(config.commerce.freeShippingThreshold)}`}</p>
          </div>
          <div
            className="hero-rise absolute right-10 bottom-24 rounded-2xl border border-white/10 bg-white/10 px-4 py-3 backdrop-blur-md"
            style={{ animationDelay: '900ms' }}
          >
            <p className="text-xs uppercase tracking-widest text-stone-300">M-Pesa & cards</p>
            <p className="text-sm font-semibold">Pay how you like</p>
          </div>
        </div>
      </div>

      <div aria-hidden className="pointer-events-none absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-black/40 to-transparent" />
    </section>
  );
}

function PerkTicker() {
  return (
    <div className="border-y border-stone-800 bg-stone-900 py-3.5 text-stone-300">
      <div className="relative flex overflow-hidden">
        <div className="animate-marquee flex shrink-0 items-center gap-12 pr-12">
          {[...PERKS, ...PERKS].map((perk, i) => (
            <span key={i} className="flex items-center gap-2.5 whitespace-nowrap text-sm">
              <perk.icon className="h-4 w-4 text-primary-300" />
              {perk.label}
            </span>
          ))}
        </div>
        <div className="animate-marquee flex shrink-0 items-center gap-12 pr-12" aria-hidden>
          {[...PERKS, ...PERKS].map((perk, i) => (
            <span key={i} className="flex items-center gap-2.5 whitespace-nowrap text-sm">
              <perk.icon className="h-4 w-4 text-primary-300" />
              {perk.label}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}

function CategoriesSection({ categories, loading }: { categories: Category[]; loading: boolean }) {
  return (
    <section className="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
      <Reveal>
        <div className="mb-10 flex items-end justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary-600">Departments</p>
            <h2 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">Shop by category</h2>
          </div>
          <Link href="/products" className="group hidden items-center gap-1.5 text-sm font-semibold text-primary-600 hover:text-primary-700 sm:flex">
            All products
            <FiArrowUpRight className="h-4 w-4 transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5" />
          </Link>
        </div>
      </Reveal>

      {loading ? (
        <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="h-44 animate-pulse rounded-2xl bg-stone-200/70" />
          ))}
        </div>
      ) : categories.length === 0 ? (
        <p className="py-8 text-center text-stone-500">No categories yet.</p>
      ) : (
        <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
          {categories.slice(0, 5).map((cat, i) => (
            <Reveal key={cat.id} delay={i * 90} className={i === 0 ? 'col-span-2 row-span-2' : ''}>
              <Link
                href={`/products?category=${cat.slug}`}
                className={`group relative flex h-full flex-col justify-end overflow-hidden rounded-2xl border border-stone-200/80 ${
                  i === 0 ? 'min-h-[13rem] md:min-h-[19rem]' : 'min-h-[9.5rem]'
                }`}
              >
                <img
                  src={`https://picsum.photos/seed/${cat.slug}-${i}/800/${i === 0 ? '900' : '480'}`}
                  alt=""
                  loading="lazy"
                  className="img-zoom absolute inset-0 h-full w-full object-cover"
                />
                <div aria-hidden className="absolute inset-0 bg-gradient-to-t from-stone-950/85 via-stone-950/25 to-transparent transition-opacity duration-500 group-hover:from-stone-950/95" />
                <div className="relative p-5">
                  <p className={`mb-1 font-semibold uppercase tracking-[0.18em] text-white/60 ${i === 0 ? 'text-xs' : 'text-[10px]'}`}>
                    {String(i + 1).padStart(2, '0')}
                  </p>
                  <h3 className={`font-bold leading-snug text-white ${i === 0 ? 'text-2xl md:text-3xl' : 'text-base'}`}>
                    {cat.name}
                  </h3>
                  <p className="mt-1 flex items-center gap-1.5 text-sm text-white/75">
                    {cat.productCount ?? 0} products
                    <FiArrowUpRight className="transition-transform duration-300 group-hover:-translate-y-0.5 group-hover:translate-x-0.5" />
                  </p>
                </div>
              </Link>
            </Reveal>
          ))}
        </div>
      )}
    </section>
  );
}

function FeaturedSection({ featured, loading }: { featured: Product[]; loading: boolean }) {
  return (
    <section className="bg-white py-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <Reveal>
          <div className="mb-10 flex items-end justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary-600">Fresh in</p>
              <h2 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">Featured products</h2>
            </div>
            <Link href="/products?sortBy=newest" className="group hidden items-center gap-1.5 text-sm font-semibold text-primary-600 hover:text-primary-700 sm:flex">
              View all
              <FiArrowUpRight className="h-4 w-4 transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5" />
            </Link>
          </div>
        </Reveal>

        {loading ? (
          <div className="grid grid-cols-2 gap-5 lg:grid-cols-4">
            {[...Array(8)].map((_, i) => (
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
        ) : featured.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-stone-300 py-16 text-center">
            <p className="text-lg font-medium text-stone-700">No products yet</p>
            <p className="mt-1 text-sm text-stone-500">Check back soon - the shelves are being stocked.</p>
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-5 lg:grid-cols-4">
            {featured.map((product, i) => (
              <Reveal key={product.id} delay={(i % 4) * 90}>
                <ProductCard product={product} />
              </Reveal>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

function SaleBanner() {
  return (
    <section className="mx-auto max-w-7xl px-4 pb-24 pt-4 sm:px-6 lg:px-8">
      <Reveal>
        <div className="relative overflow-hidden rounded-3xl bg-stone-950 px-8 py-14 text-white sm:px-14">
          <div
            aria-hidden
            className="absolute inset-0"
            style={{
              background:
                'radial-gradient(38rem 22rem at 88% 120%, oklch(0.5 0.15 255 / 0.5), transparent 65%)',
            }}
          />
          <div className="relative max-w-xl">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary-300">Limited time</p>
            <h2 className="mt-3 text-3xl font-bold tracking-tight sm:text-4xl">
              Sale picks, real savings.
            </h2>
            <p className="mt-3 text-stone-300">
              Marked-down pieces from across the catalogue. When they are gone, they are gone.
            </p>
            <Link
              href="/products?onSaleOnly=true"
              className="group mt-7 inline-flex items-center gap-2 rounded-xl bg-white px-6 py-3 font-semibold text-stone-900 transition-all hover:-translate-y-0.5 hover:bg-primary-50"
            >
              Shop the sale
              <FiArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
            </Link>
          </div>
        </div>
      </Reveal>
    </section>
  );
}
