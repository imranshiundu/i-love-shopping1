'use client';
import { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { admin } from '@/services/api';
import { formatKES, formatDate, ORDER_STATUSES } from '@/lib/utils';
import Reveal from '@/components/ui/Reveal';
import StatusBadge from '@/components/admin/StatusBadge';
import { FiChevronLeft, FiChevronRight, FiX, FiExternalLink } from 'react-icons/fi';
import toast from 'react-hot-toast';

const NEXT_ACTIONS: Record<string, string[]> = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['PROCESSING', 'CANCELLED'],
  PROCESSING: ['SHIPPED', 'CANCELLED'],
  SHIPPED: ['DELIVERED'],
  DELIVERED: ['REFUNDED'],
  CANCELLED: [],
  REFUNDED: [],
};

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<any[]>([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await admin.listOrders(currentPage, 15, statusFilter || undefined);
      setOrders(res.data?.content || []);
      setTotalPages(res.data?.totalPages || 1);
      setTotalElements(res.data?.totalElements || 0);
    } catch (e: any) { toast.error(e.message); }
    setLoading(false);
  }, [currentPage, statusFilter]);

  useEffect(() => { load(); }, [load]);

  const updateStatus = async (orderNumber: string, status: string) => {
    setUpdatingId(orderNumber);
    try {
      await admin.updateOrderStatus(orderNumber, status);
      toast.success(`${orderNumber} is now ${status}`);
      await load();
    } catch (e: any) { toast.error(e.message); }
    setUpdatingId(null);
  };

  return (
    <div className="space-y-6">
      <Reveal>
        <div>
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Orders</h1>
          <p className="mt-1 text-sm text-stone-500">{totalElements} total - move each one through its lifecycle.</p>
        </div>
      </Reveal>

      <Reveal delay={60}>
        <div className="no-scrollbar flex gap-2 overflow-x-auto pb-1">
          <FilterChip label="All" active={!statusFilter} onClick={() => { setStatusFilter(''); setCurrentPage(0); }} />
          {ORDER_STATUSES.map(s => (
            <FilterChip key={s} label={s} active={statusFilter === s}
              onClick={() => { setStatusFilter(s); setCurrentPage(0); }} />
          ))}
        </div>
      </Reveal>

      <Reveal delay={100}>
        <div className="overflow-hidden rounded-2xl border border-stone-200/80 bg-white">
          {loading ? (
            <div className="space-y-3 p-6">{[...Array(6)].map((_, i) => <div key={i} className="h-12 animate-pulse rounded-xl bg-stone-200/50" />)}</div>
          ) : orders.length === 0 ? (
            <p className="p-14 text-center text-sm text-stone-500">No orders with this status.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[720px] text-sm">
                <thead>
                  <tr className="border-b border-stone-100 text-left text-xs uppercase tracking-wider text-stone-400">
                    <th className="px-5 py-3.5 font-semibold">Order</th>
                    <th className="px-5 py-3.5 font-semibold">Placed</th>
                    <th className="px-5 py-3.5 font-semibold">Status</th>
                    <th className="px-5 py-3.5 font-semibold">Customer ID</th>
                    <th className="px-5 py-3.5 text-right font-semibold">Total</th>
                    <th className="px-5 py-3.5 font-semibold">Move to</th>
                  </tr>
                </thead>
                <tbody className={updatingId ? 'opacity-60' : ''}>
                  {orders.map(o => (
                    <tr key={o.id} className="border-b border-stone-50 last:border-0 hover:bg-stone-50/60">
                      <td className="px-5 py-3.5 font-mono text-xs font-semibold">{o.number}</td>
                      <td className="px-5 py-3.5 text-stone-500">{formatDate(o.createdAt)}</td>
                      <td className="px-5 py-3.5"><StatusBadge status={o.status} /></td>
                      <td className="max-w-[140px] truncate px-5 py-3.5 font-mono text-xs text-stone-400">{o.userId}</td>
                      <td className="px-5 py-3.5 text-right font-semibold tabular-nums">{formatKES(o.total)}</td>
                      <td className="px-5 py-3.5">
                        <div className="flex flex-wrap gap-1.5">
                          {(NEXT_ACTIONS[o.status] || []).map(action => (
                            <button key={action} onClick={() => updateStatus(o.number, action)}
                              disabled={updatingId === o.number}
                              className={`rounded-lg px-2.5 py-1 text-xs font-bold transition-colors ${
                                action === 'CANCELLED'
                                  ? 'bg-rose-50 text-rose-700 hover:bg-rose-100'
                                  : action === 'REFUNDED'
                                    ? 'bg-stone-100 text-stone-700 hover:bg-stone-200'
                                    : 'bg-primary-50 text-primary-700 hover:bg-primary-100'
                              }`}>
                              {action}
                            </button>
                          ))}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {totalPages > 1 && !loading && (
            <div className="flex items-center justify-between border-t border-stone-100 px-5 py-3">
              <button onClick={() => setCurrentPage(p => Math.max(0, p - 1))} disabled={currentPage === 0}
                className="flex items-center gap-1 rounded-lg border px-3 py-1.5 text-xs font-medium disabled:opacity-40">
                <FiChevronLeft /> Prev
              </button>
              <span className="text-xs text-stone-500">Page {currentPage + 1} of {totalPages}</span>
              <button onClick={() => setCurrentPage(p => Math.min(totalPages - 1, p + 1))} disabled={currentPage >= totalPages - 1}
                className="flex items-center gap-1 rounded-lg border px-3 py-1.5 text-xs font-medium disabled:opacity-40">
                Next <FiChevronRight />
              </button>
            </div>
          )}
        </div>
      </Reveal>
    </div>
  );
}

function FilterChip({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button onClick={onClick}
      className={`shrink-0 rounded-full px-4 py-1.5 text-xs font-bold transition-colors ${
        active ? 'bg-stone-900 text-white' : 'bg-white text-stone-600 ring-1 ring-stone-200 hover:bg-stone-50'
      }`}>
      {label}
    </button>
  );
}
