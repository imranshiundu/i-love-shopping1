'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useAuth } from '@/contexts/AuthContext';
import { orders as ordersApi } from '@/services/api';
import { config } from '@/lib/config';
import { formatKES, formatDate } from '@/lib/utils';
import Reveal from '@/components/ui/Reveal';
import StatusBadge from '@/components/admin/StatusBadge';
import {
  FiPackage, FiDollarSign, FiClock, FiMapPin, FiArrowUpRight,
  FiSettings, FiShoppingBag, FiTruck,
} from 'react-icons/fi';

interface OrderLite {
  id: string; number: string; status: string; total: number; createdAt: string;
  items?: { id: string; productName: string; productImage?: string; quantity: number }[];
}

export default function AccountDashboardPage() {
  const { user } = useAuth();
  const [orders, setOrders] = useState<OrderLite[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    ordersApi.list(0, 50).then(res => {
      const list: any[] = (res.data as any)?.orders || (Array.isArray(res.data) ? res.data : []);
      setOrders(list);
    }).catch(() => {}).finally(() => setLoading(false));
  }, []);

  const paid = orders.filter(o => ['CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED'].includes(o.status));
  const lifetimeSpend = paid.reduce((sum, o) => sum + (o.total || 0), 0);
  const activeOrders = orders.filter(o => ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED'].includes(o.status));
  const firstName = user?.name?.split(' ')[0] || 'there';

  return (
    <div className="space-y-6">
      <Reveal>
        <section className="relative overflow-hidden rounded-3xl bg-stone-950 p-7 text-white sm:p-9">
          <div
            aria-hidden
            className="absolute inset-0"
            style={{ background: 'radial-gradient(38rem 20rem at 88% -30%, oklch(0.45 0.14 250 / 0.5), transparent 62%)' }}
          />
          <div className="relative flex flex-wrap items-center justify-between gap-6">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary-300">Welcome back</p>
              <h1 className="mt-1.5 text-3xl font-bold tracking-tight">{firstName}.</h1>
              <p className="mt-1 text-sm text-stone-400">
                {activeOrders.length > 0
                  ? `You have ${activeOrders.length} order${activeOrders.length === 1 ? '' : 's'} in progress.`
                  : 'Your next favourite thing is a click away.'}
              </p>
            </div>
            <Link href="/products"
              className="group inline-flex items-center gap-2 rounded-xl bg-primary-500 px-5 py-3 text-sm font-semibold shadow-lg shadow-primary-500/25 transition-all hover:-translate-y-0.5 hover:bg-primary-400">
              Keep shopping
              <FiArrowUpRight className="transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5" />
            </Link>
          </div>
        </section>
      </Reveal>

      <div className="grid grid-cols-2 gap-4 xl:grid-cols-4">
        <StatCard delay={0} icon={FiShoppingBag} tone="primary" label="Total orders" value={String(orders.length)} />
        <StatCard delay={70} icon={FiDollarSign} tone="emerald" label="Lifetime spend" value={formatKES(lifetimeSpend)} />
        <StatCard delay={140} icon={FiTruck} tone="amber" label="In progress" value={String(activeOrders.length)} />
        <StatCard delay={210} icon={FiClock} tone="rose" label="Awaiting payment" value={String(orders.filter(o => o.status === 'PENDING').length)} />
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.6fr_1fr]">
        <Reveal delay={100}>
          <section className="overflow-hidden rounded-2xl border border-stone-200/80 bg-white">
            <header className="flex items-center justify-between border-b border-stone-100 px-6 py-4">
              <h2 className="font-bold">Recent orders</h2>
              <Link href="/account/orders" className="flex items-center gap-1 text-sm font-semibold text-primary-600 hover:text-primary-700">
                View all <FiArrowUpRight />
              </Link>
            </header>
            {loading ? (
              <div className="space-y-3 p-5">{[...Array(3)].map((_, i) => <div key={i} className="h-14 animate-pulse rounded-xl bg-stone-200/50" />)}</div>
            ) : orders.length === 0 ? (
              <div className="px-6 py-14 text-center">
                <FiPackage className="mx-auto h-8 w-8 text-stone-300" />
                <p className="mt-4 font-semibold text-stone-700">No orders yet</p>
                <p className="mt-1 text-sm text-stone-500">Your purchases will appear here with live status.</p>
                <Link href="/products" className="mt-5 inline-block rounded-xl bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-700">
                  Browse products
                </Link>
              </div>
            ) : (
              <ul>
                {orders.slice(0, 5).map(o => (
                  <li key={o.id} className="flex items-center gap-4 border-b border-stone-50 px-6 py-4 last:border-0 hover:bg-stone-50/60">
                    <span className="h-11 w-11 shrink-0 overflow-hidden rounded-lg bg-stone-100 ring-1 ring-stone-200">
                      {o.items?.[0]?.productImage && <img src={o.items[0].productImage} alt="" className="h-full w-full object-cover" />}
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="font-mono text-xs font-semibold text-stone-500">{o.number}</p>
                      <p className="text-sm">{formatDate(o.createdAt)}</p>
                    </div>
                    <StatusBadge status={o.status} />
                    <span className="hidden w-24 text-right text-sm font-bold tabular-nums sm:block">{formatKES(o.total)}</span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </Reveal>

        <Reveal delay={160}>
          <aside className="space-y-4">
            <div className="rounded-2xl border border-stone-200/80 bg-white p-6">
              <h3 className="font-bold">Quick actions</h3>
              <nav className="mt-4 space-y-2">
                <QuickAction href="/account/settings" icon={FiSettings} label="Account settings" note="Profile and security" />
                <QuickAction href="/account/addresses" icon={FiMapPin} label="Address book" note="Delivery destinations" />
                <QuickAction href="/cart" icon={FiShoppingBag} label="Your cart" note="Pick up where you left off" />
              </nav>
            </div>

            <div className="rounded-2xl bg-gradient-to-br from-stone-900 to-stone-950 p-6 text-white">
              <FiTruck className="h-6 w-6 text-primary-300" />
              <p className="mt-3 font-bold">Free delivery</p>
              <p className="mt-1 text-sm leading-relaxed text-stone-400">
                Orders over {formatKES(config.commerce.freeShippingThreshold)} ship free,
                nationwide in 1-3 business days.
              </p>
            </div>
          </aside>
        </Reveal>
      </div>
    </div>
  );
}

function StatCard({ icon: Icon, label, value, tone, delay }: {
  icon: any; label: string; value: string; tone: 'primary' | 'emerald' | 'amber' | 'rose'; delay: number;
}) {
  const tones = {
    primary: 'bg-primary-50 text-primary-600',
    emerald: 'bg-emerald-50 text-emerald-600',
    amber: 'bg-amber-50 text-amber-600',
    rose: 'bg-rose-50 text-rose-600',
  };
  return (
    <Reveal delay={delay}>
      <div className="card-lift rounded-2xl border border-stone-200/80 bg-white p-5">
        <div className="flex items-start justify-between">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-stone-400">{label}</p>
          <span className={`rounded-xl p-2 ${tones[tone]}`}><Icon className="h-4 w-4" /></span>
        </div>
        <p className="mt-3 text-xl font-extrabold tabular-nums tracking-tight sm:text-2xl">{value}</p>
      </div>
    </Reveal>
  );
}

function QuickAction({ href, icon: Icon, label, note }: { href: string; icon: any; label: string; note: string }) {
  return (
    <Link href={href}
      className="group flex items-center justify-between rounded-xl border border-stone-100 p-3.5 transition-colors hover:border-primary-200 hover:bg-primary-50/40">
      <span className="flex items-center gap-3">
        <span className="rounded-lg bg-stone-100 p-2 text-stone-700"><Icon className="h-4 w-4" /></span>
        <span>
          <span className="block text-sm font-semibold">{label}</span>
          <span className="block text-xs text-stone-500">{note}</span>
        </span>
      </span>
      <FiArrowUpRight className="h-4 w-4 text-stone-400 transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5" />
    </Link>
  );
}
