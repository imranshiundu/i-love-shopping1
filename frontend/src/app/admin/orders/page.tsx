'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { admin } from '@/services/api';
import { Order } from '@/types';
import { formatKES, formatDate, statusColor, ORDER_STATUSES } from '@/lib/utils';
import toast from 'react-hot-toast';

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try { const res = await admin.listOrders(0, 50); setOrders(res.data?.orders || []); }
    catch { toast.error('Failed to load orders'); }
    setLoading(false);
  };
  useEffect(() => { load(); }, []);

  const updateStatus = async (orderNumber: string, status: string) => {
    try { await admin.updateOrderStatus(orderNumber, status); toast.success('Status updated'); load(); }
    catch (e: any) { toast.error(e.message); }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-3xl font-bold">Manage Orders</h1>
        <Link href="/admin" className="text-primary-600 hover:underline text-sm">&larr; Back to Dashboard</Link>
      </div>

      {loading ? (
        <div className="space-y-4">{[...Array(5)].map((_, i) => <div key={i} className="bg-white rounded-lg h-16 animate-pulse" />)}</div>
      ) : (
        <div className="bg-white rounded-lg border overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="px-4 py-3 text-left">Order</th>
                <th className="px-4 py-3 text-left">Date</th>
                <th className="px-4 py-3 text-left">Items</th>
                <th className="px-4 py-3 text-left">Total</th>
                <th className="px-4 py-3 text-left">Status</th>
                <th className="px-4 py-3 text-left">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {orders.map(order => (
                <tr key={order.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium">{order.number}</td>
                  <td className="px-4 py-3 text-gray-500">{formatDate(order.createdAt)}</td>
                  <td className="px-4 py-3">{order.items?.length || 0}</td>
                  <td className="px-4 py-3 font-medium">{formatKES(order.total)}</td>
                  <td className="px-4 py-3"><span className={`text-xs px-2 py-1 rounded-full ${statusColor(order.status)}`}>{order.status}</span></td>
                  <td className="px-4 py-3">
                    <select value={order.status} onChange={e => updateStatus(order.number, e.target.value)}
                      className="border rounded px-2 py-1 text-xs">
                      {ORDER_STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
