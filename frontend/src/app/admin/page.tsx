'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { admin, products as productsApi } from '@/services/api';
import { config } from '@/lib/config';
import { Product } from '@/types';
import { formatKES, formatDate, statusColor, ORDER_STATUSES } from '@/lib/utils';
import Reveal from '@/components/ui/Reveal';
import StatusBadge from '@/components/admin/StatusBadge';
import {
  FiDollarSign, FiShoppingBag, FiUsers, FiPackage, FiTrendingUp,
  FiXCircle, FiRotateCcw, FiClock, FiArrowUpRight, FiAlertTriangle, FiActivity,
} from 'react-icons/fi';

interface Stats {
  totalRevenue: number;
  cancelledValue: number;
  refundedValue: number;
  averageOrderValue: number;
  pendingValue: number;
  totalOrders: number;
  totalCustomers: number;
  totalProducts: number;
  activeProducts: number;
  pendingOrders: number;
  ordersByStatus: Record<string, number>;
  revenueByDay: { date: string; revenue: number; orders: number }[];
  topProducts: { name: string; slug: string; unitsSold: number; revenue: number }[];
  lowStock: { id: string; name: string; stock: number; price: number }[];
  recentOrders: any[];
}

export default function AdminDashboard() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [catalog, setCatalog] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([admin.getStats(), productsApi.search({ page: '0', size: '100' })])
      .then(([s, p]) => {
        setStats(s.data);
        setCatalog(p.data?.products || []);
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <DashboardSkeleton />;
  if (!stats) return <p className="p-8 text-stone-500">Could not load statistics.</p>;

  const maxRevenue = Math.max(...stats.revenueByDay.map(d => d.revenue), 1);

  return (
    <div className="space-y-6">
      <Reveal>
        <div>
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Business overview</h1>
          <p className="mt-1 text-sm text-stone-500">Everything happening in your store, right now.</p>
        </div>
      </Reveal>

      <div className="grid grid-cols-2 gap-4 xl:grid-cols-4">
        <KpiCard delay={0} label="Collected revenue" value={formatKES(stats.totalRevenue)} icon={FiDollarSign}
          sub={`${stats.totalOrders} orders all-time`} tone="emerald" />
        <KpiCard delay={80} label="Awaiting payment" value={formatKES(stats.pendingValue)} icon={FiClock}
          sub={`${stats.pendingOrders} pending orders`} tone="amber" />
        <KpiCard delay={160} label="Average order" value={formatKES(stats.averageOrderValue)} icon={FiTrendingUp}
          sub="Across paid orders" tone="primary" />
        <KpiCard delay={240} label="Lost to cancellations" value={formatKES(stats.cancelledValue)} icon={FiXCircle}
          sub={`${formatKES(stats.refundedValue)} refunded`} tone="rose" />
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.7fr_1fr]">
        <Reveal delay={100}>
          <section className="rounded-2xl border border-stone-200/80 bg-white p-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="font-bold">Revenue, last 7 days</h2>
                <p className="text-xs text-stone-500">Paid orders only</p>
              </div>
              <FiActivity className="h-5 w-5 text-primary-600" />
            </div>
            <div className="mt-6 flex h-44 items-end gap-3">
              {stats.revenueByDay.map((day, i) => (
                <div key={day.date} className="group relative flex flex-1 flex-col items-center gap-2">
                  <span className="pointer-events-none absolute -top-7 rounded-md bg-stone-900 px-2 py-1 text-[10px] font-semibold text-white opacity-0 transition-opacity group-hover:opacity-100 whitespace-nowrap">
                    {formatKES(day.revenue)}
                  </span>
                  <div
                    className="w-full rounded-t-lg bg-gradient-to-t from-primary-600/70 to-primary-400 transition-all duration-700 hover:from-primary-600 hover:to-primary-300"
                    style={{ height: `${Math.max(4, (day.revenue / maxRevenue) * 140)}px`, transitionDelay: `${i * 60}ms` }}
                  />
                  <span className="text-[11px] text-stone-500">{day.date.slice(5)}</span>
                </div>
              ))}
            </div>
            <div className="mt-4 grid grid-cols-3 divide-x divide-stone-100 border-t border-stone-100 pt-4 text-center">
              <MiniStat label="Best day" value={bestDayLabel(stats.revenueByDay)} />
              <MiniStat label="Orders this week" value={String(stats.revenueByDay.reduce((a, d) => a + d.orders, 0))} />
              <MiniStat label="Live catalogue" value={`${stats.activeProducts} of ${stats.totalProducts}`} />
            </div>
          </section>
        </Reveal>

        <Reveal delay={180}>
          <section className="rounded-2xl border border-stone-200/80 bg-white p-6">
            <h2 className="font-bold">Orders by status</h2>
            <ul className="mt-4 space-y-2.5">
              {ORDER_STATUSES.filter(s => stats.ordersByStatus[s] > 0 || s === 'PENDING').map(status => {
                const count = stats.ordersByStatus[status] || 0;
                const pct = stats.totalOrders ? Math.round((count / stats.totalOrders) * 100) : 0;
                return (
                  <li key={status} className="flex items-center gap-3 text-sm">
                    <span className={`inline-block w-20 shrink-0 rounded-full px-2 py-0.5 text-center text-[10px] font-bold ${statusColor(status)}`}>
                      {status}
                    </span>
                    <div className="h-2 flex-1 overflow-hidden rounded-full bg-stone-100">
                      <div className={`h-full rounded-full transition-all duration-700 ${status.includes('CANCEL') || status.includes('REFUND') ? 'bg-rose-400' : status === 'DELIVERED' ? 'bg-emerald-400' : 'bg-primary-400'}`}
                        style={{ width: `${Math.max(pct, count > 0 ? 6 : 0)}%` }} />
                    </div>
                    <span className="w-7 text-right font-semibold tabular-nums">{count}</span>
                  </li>
                );
              })}
            </ul>
            <Link href="/admin/orders" className="mt-5 flex items-center justify-center gap-1.5 rounded-xl bg-stone-900 py-2.5 text-sm font-semibold text-white hover:bg-stone-800">
              Manage orders <FiArrowUpRight />
            </Link>
          </section>
        </Reveal>
      </div>

      <div className="grid gap-6 xl:grid-cols-3">
        <Reveal delay={120}>
          <section className="rounded-2xl border border-stone-200/80 bg-white p-6">
            <h2 className="flex items-center gap-2 font-bold"><FiTrendingUp className="text-emerald-600" /> Best sellers</h2>
            {stats.topProducts.length === 0 ? (
              <p className="mt-4 text-sm text-stone-500">No paid orders yet.</p>
            ) : (
              <ol className="mt-4 space-y-3">
                {stats.topProducts.map((tp, i) => (
                  <li key={tp.name} className="flex items-center gap-3">
                    <span className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-lg text-xs font-bold ${
                      i === 0 ? 'bg-amber-100 text-amber-700' : i === 1 ? 'bg-stone-200 text-stone-700' : i === 2 ? 'bg-orange-100 text-orange-700' : 'bg-stone-100 text-stone-500'
                    }`}>{i + 1}</span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium">{tp.name}</p>
                      <p className="text-xs text-stone-500">{tp.unitsSold} sold</p>
                    </div>
                    <span className="text-sm font-bold tabular-nums">{formatKES(tp.revenue)}</span>
                  </li>
                ))}
              </ol>
            )}
          </section>
        </Reveal>

        <Reveal delay={180}>
          <section className="rounded-2xl border border-stone-200/80 bg-white p-6">
            <h2 className="flex items-center gap-2 font-bold"><FiAlertTriangle className="text-amber-600" /> Low stock</h2>
            {stats.lowStock.length === 0 ? (
              <p className="mt-4 text-sm text-stone-500">All products healthy.</p>
            ) : (
              <ul className="mt-4 space-y-3">
                {stats.lowStock.map(p => (
                  <li key={p.id} className="flex items-center justify-between gap-3">
                    <p className="min-w-0 flex-1 truncate text-sm">{p.name}</p>
                    <span className={`rounded-full px-2.5 py-0.5 text-xs font-bold ${p.stock === 0 ? 'bg-rose-100 text-rose-700' : 'bg-amber-100 text-amber-700'}`}>
                      {p.stock} left
                    </span>
                  </li>
                ))}
              </ul>
            )}
            <Link href="/admin/offers" className="mt-5 flex items-center justify-center gap-1.5 rounded-xl border border-stone-200 py-2.5 text-sm font-semibold hover:bg-stone-50">
              Run a promotion
            </Link>
          </section>
        </Reveal>

        <Reveal delay={240}>
          <section className="rounded-2xl border border-stone-200/80 bg-white p-6">
            <h2 className="flex items-center gap-2 font-bold"><FiPackage className="text-primary-600" /> Catalogue health</h2>
            <dl className="mt-4 space-y-3 text-sm">
              <Row icon={FiPackage} label="Active products" value={String(stats.activeProducts)} />
              <Row icon={FiUsers} label="Customers" value={String(stats.totalCustomers)} />
              <Row icon={FiShoppingBag} label="Total orders" value={String(stats.totalOrders)} />
              <Row icon={FiClock} label="Awaiting payment" value={String(stats.pendingOrders)} />
            </dl>
            <div className="mt-5 grid grid-cols-2 gap-2">
              <Link href="/admin/products" className="rounded-xl border border-stone-200 py-2.5 text-center text-sm font-semibold hover:bg-stone-50">Products</Link>
              <Link href="/admin/users" className="rounded-xl border border-stone-200 py-2.5 text-center text-sm font-semibold hover:bg-stone-50">Users</Link>
            </div>
          </section>
        </Reveal>
      </div>

      <Reveal delay={120}>
        <section className="overflow-hidden rounded-2xl border border-stone-200/80 bg-white">
          <div className="border-b border-stone-100 p-6 pb-4">
            <h2 className="font-bold">Latest orders</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-stone-100 text-left text-xs uppercase tracking-wider text-stone-400">
                  <th className="px-6 py-3 font-semibold">Order</th>
                  <th className="px-6 py-3 font-semibold">Date</th>
                  <th className="px-6 py-3 font-semibold">Status</th>
                  <th className="px-6 py-3 text-right font-semibold">Total</th>
                </tr>
              </thead>
              <tbody>
                {stats.recentOrders.map(o => (
                  <tr key={o.id} className="border-b border-stone-50 last:border-0 hover:bg-stone-50/60">
                    <td className="px-6 py-3.5 font-mono text-xs font-semibold">{o.number}</td>
                    <td className="px-6 py-3.5 text-stone-500">{formatDate(o.createdAt)}</td>
                    <td className="px-6 py-3.5"><StatusBadge status={o.status} /></td>
                    <td className="px-6 py-3.5 text-right font-semibold tabular-nums">{formatKES(o.total)}</td>
                  </tr>
                ))}
                {stats.recentOrders.length === 0 && (
                  <tr><td colSpan={4} className="px-6 py-10 text-center text-stone-500">No orders yet.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      </Reveal>
    </div>
  );
}

function KpiCard({ label, value, sub, icon: Icon, tone, delay }: {
  label: string; value: string; sub?: string; icon: any; tone: 'emerald' | 'amber' | 'primary' | 'rose'; delay: number;
}) {
  const tones = {
    emerald: 'bg-emerald-50 text-emerald-600',
    amber: 'bg-amber-50 text-amber-600',
    primary: 'bg-primary-50 text-primary-600',
    rose: 'bg-rose-50 text-rose-600',
  };
  return (
    <Reveal delay={delay}>
      <div className="card-lift rounded-2xl border border-stone-200/80 bg-white p-5">
        <div className="flex items-start justify-between">
          <p className="text-xs font-semibold uppercase tracking-wider text-stone-400">{label}</p>
          <span className={`rounded-xl p-2 ${tones[tone]}`}><Icon className="h-4 w-4" /></span>
        </div>
        <p className="mt-3 text-xl font-extrabold tabular-nums tracking-tight sm:text-2xl">{value}</p>
        {sub && <p className="mt-1 text-xs text-stone-500">{sub}</p>}
      </div>
    </Reveal>
  );
}

function MiniStat({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-[11px] uppercase tracking-wider text-stone-400">{label}</p>
      <p className="mt-0.5 text-sm font-bold">{value}</p>
    </div>
  );
}

function Row({ icon: Icon, label, value }: { icon: any; label: string; value: string }) {
  return (
    <div className="flex items-center justify-between">
      <dt className="flex items-center gap-2 text-stone-500"><Icon className="h-4 w-4" />{label}</dt>
      <dd className="font-bold tabular-nums">{value}</dd>
    </div>
  );
}

function bestDayLabel(days: { date: string; revenue: number }[]): string {
  const best = days.reduce((a, b) => (b.revenue > a.revenue ? b : a), days[0] || { date: '-', revenue: 0 });
  return best.date.slice(5);
}

function DashboardSkeleton() {
  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-4 xl:grid-cols-4">
        {[...Array(4)].map((_, i) => <div key={i} className="h-28 animate-pulse rounded-2xl bg-stone-200/60" />)}
      </div>
      <div className="grid gap-6 xl:grid-cols-[1.7fr_1fr]">
        <div className="h-72 animate-pulse rounded-2xl bg-stone-200/60" />
        <div className="h-72 animate-pulse rounded-2xl bg-stone-200/60" />
      </div>
      <div className="h-64 animate-pulse rounded-2xl bg-stone-200/60" />
    </div>
  );
}
