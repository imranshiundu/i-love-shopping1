'use client';
import { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { orders as ordersApi } from '@/services/api';
import { config } from '@/lib/config';
import { formatKES, formatDate, ORDER_STATUSES } from '@/lib/utils';
import Reveal from '@/components/ui/Reveal';
import { FiPackage, FiChevronRight, FiXCircle, FiClock, FiCheckCircle, FiTruck, FiBox, FiRefreshCw } from 'react-icons/fi';
import toast from 'react-hot-toast';

const STATUS_ICON: Record<string, any> = {
  PENDING: FiClock,
  CONFIRMED: FiCheckCircle,
  PROCESSING: FiBox,
  SHIPPED: FiTruck,
  DELIVERED: FiCheckCircle,
  CANCELLED: FiXCircle,
  REFUNDED: FiRefreshCw,
};

const FILTERS = ['ALL', ...ORDER_STATUSES];

export default function AccountOrdersPage() {
  const [orders, setOrders] = useState<any[]>([]);
  const [filter, setFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [cancellingId, setCancellingId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await ordersApi.list(0, 50);
      const list: any[] = (res.data as any)?.orders || (Array.isArray(res.data) ? res.data : []);
      setOrders(list);
    } catch { setOrders([]); }
    setLoading(false);
  }, []);

  useEffect(() => { load(); }, [load]);

  const cancelOrder = async (number: string) => {
    if (!confirm(`Cancel order ${number}? Stock will be returned to the catalogue.`)) return;
    setCancellingId(number);
    try {
      await ordersApi.cancel(number);
      toast.success(`Order ${number} cancelled`);
      await load();
    } catch (e: any) { toast.error(e.message); }
    setCancellingId(null);
  };

  const visible = filter === 'ALL' ? orders : orders.filter(o => o.status === filter);
  const spent = orders.filter(o => !['CANCELLED', 'REFUNDED', 'PENDING'].includes(o.status))
    .reduce((sum, o) => sum + (o.total || 0), 0);

  return (
    <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
      <Reveal>
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary-600">Order history</p>
          <h1 className="mt-1.5 text-3xl font-bold tracking-tight">Your purchases</h1>
          <p className="mt-1.5 flex flex-wrap gap-x-4 gap-y-1 text-sm text-stone-500">
            <span>{orders.length} orders</span>
            <span className="text-stone-300">|</span>
            <span>{formatKES(spent)} lifetime spend</span>
            <span className="text-stone-300">|</span>
            <span>Free delivery over {formatKES(config.commerce.freeShippingThreshold)}</span>
          </p>
        </div>
      </Reveal>

      <Reveal delay={70}>
        <div className="no-scrollbar mt-7 flex gap-2 overflow-x-auto pb-1">
          {FILTERS.map(f => (
            <button key={f} onClick={() => setFilter(f)}
              className={`shrink-0 rounded-full px-4 py-1.5 text-xs font-bold transition-colors ${
                filter === f ? 'bg-stone-900 text-white' : 'bg-white text-stone-600 ring-1 ring-stone-200 hover:bg-stone-50'
              }`}>
              {f === 'ALL' ? `All (${orders.length})` : f}
            </button>
          ))}
        </div>
      </Reveal>

      <div className="mt-6 space-y-4">
        {loading ? (
          [...Array(3)].map((_, i) => <div key={i} className="h-40 animate-pulse rounded-2xl bg-white" />)
        ) : visible.length === 0 ? (
          <Reveal>
            <div className="rounded-3xl border border-dashed border-stone-300 bg-white px-6 py-20 text-center">
              <span className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-stone-100">
                <FiPackage className="h-7 w-7 text-stone-400" />
              </span>
              <p className="mt-5 text-lg font-semibold text-stone-800">
                {filter === 'ALL' ? 'No orders yet' : `No ${filter.toLowerCase()} orders`}
              </p>
              <p className="mt-1 text-sm text-stone-500">When you place an order it will show up here with live status.</p>
              <Link href="/products" className="mt-6 inline-flex items-center gap-2 rounded-xl bg-primary-600 px-6 py-3 font-semibold text-white hover:bg-primary-700">
                Start shopping
              </Link>
            </div>
          </Reveal>
        ) : (
          visible.map((order, idx) => {
            const items: any[] = order.items || [];
            const cancellable = ['PENDING', 'CONFIRMED'].includes(order.status);
            const StatusIcon = STATUS_ICON[order.status] || FiBox;
            return (
              <Reveal key={order.id} delay={Math.min(idx * 60, 240)}>
                <article className="card-lift overflow-hidden rounded-2xl border border-stone-200/80 bg-white">
                  <header className="flex flex-wrap items-center gap-x-6 gap-y-3 border-b border-stone-100 bg-stone-50/60 px-5 py-4">
                    <div>
                      <p className="font-mono text-xs font-bold text-stone-500">{order.number}</p>
                      <p className="text-sm font-semibold">{formatDate(order.createdAt)}</p>
                    </div>
                    <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-bold ${
                      order.status.includes('CANCEL') || order.status.includes('REFUND')
                        ? 'bg-rose-100 text-rose-700'
                        : order.status === 'DELIVERED'
                          ? 'bg-emerald-100 text-emerald-700'
                          : 'bg-primary-100 text-primary-700'
                    }`}>
                      <StatusIcon className="h-3.5 w-3.5" />
                      {order.status}
                    </span>

                    <div className="ml-auto text-right">
                      <p className="text-lg font-extrabold tabular-nums">{formatKES(order.total)}</p>
                      <p className="text-xs text-stone-400">{items.length} item{items.length === 1 ? '' : 's'}</p>
                    </div>
                  </header>

                  <div className="flex flex-wrap items-center gap-3 px-5 py-4">
                    {items.slice(0, 4).map(item => (
                      <div key={item.id} className="flex items-center gap-2.5 rounded-xl bg-stone-50 py-1.5 pl-1.5 pr-3.5">
                        <span className="h-10 w-10 shrink-0 overflow-hidden rounded-lg bg-stone-200">
                          {item.productImage && <img src={item.productImage} alt="" className="h-full w-full object-cover" />}
                        </span>
                        <span className="min-w-0">
                          <span className="block max-w-[150px] truncate text-xs font-semibold">{item.productName}</span>
                          <span className="block text-[11px] text-stone-500">x{item.quantity}</span>
                        </span>
                      </div>
                    ))}
                    {items.length > 4 && (
                      <span className="rounded-xl bg-stone-100 px-3 py-2 text-xs font-semibold text-stone-500">
                        +{items.length - 4} more
                      </span>
                    )}

                    <div className="ml-auto flex items-center gap-2">
                      <button onClick={() => cancelOrder(order.number)} disabled={!cancellable || cancellingId === order.number}
                        title={cancellable ? 'Cancel this order' : 'This order can no longer be cancelled'}
                        className={`flex items-center gap-1 rounded-lg px-3.5 py-2 text-xs font-semibold transition-colors ${
                          cancellable
                            ? 'bg-rose-50 text-rose-700 hover:bg-rose-100 disabled:opacity-50'
                            : 'cursor-not-allowed bg-stone-50 text-stone-300'
                        }`}>
                        <FiXCircle /> {cancellingId === order.number ? 'Cancelling...' : 'Cancel'}
                      </button>
                      <Link href={`/checkout/success?order=${order.number}`}
                        className="flex items-center gap-1 rounded-lg border border-stone-200 px-3.5 py-2 text-xs font-semibold hover:bg-stone-50">
                        Details <FiChevronRight />
                      </Link>
                    </div>
                  </div>
                </article>
              </Reveal>
            );
          })
        )}
      </div>
    </div>
  );
}
