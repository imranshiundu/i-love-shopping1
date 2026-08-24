'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { orders } from '@/services/api';
import { Order } from '@/types';
import { formatKES, formatDate, statusColor } from '@/lib/utils';
import { FiChevronRight } from 'react-icons/fi';

export default function OrdersPage() {
  const [orderList, setOrderList] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    orders.list(0, 50).then(res => setOrderList(res.data?.orders || [])).finally(() => setLoading(false));
  }, []);

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-8">My Orders</h1>
      {loading ? (
        <div className="space-y-4">{[...Array(3)].map((_, i) => <div key={i} className="bg-white rounded-lg h-24 animate-pulse" />)}</div>
      ) : orderList.length === 0 ? (
        <div className="text-center py-16">
          <p className="text-gray-500 text-lg mb-4">No orders yet</p>
          <Link href="/products" className="bg-primary-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-primary-700">Start Shopping</Link>
        </div>
      ) : (
        <div className="space-y-4">
          {orderList.map(order => (
            <Link key={order.id} href={`/account/orders?view=${order.number}`}
              className="bg-white rounded-lg p-6 border hover:shadow-md transition flex items-center justify-between">
              <div>
                <div className="flex items-center gap-3">
                  <span className="font-bold">{order.number}</span>
                  <span className={`text-xs px-2 py-1 rounded-full ${statusColor(order.status)}`}>{order.status}</span>
                </div>
                <p className="text-sm text-gray-500 mt-1">{formatDate(order.createdAt)} &middot; {order.items?.length || 0} items</p>
              </div>
              <div className="text-right flex items-center gap-4">
                <div>
                  <p className="font-bold">{formatKES(order.total)}</p>
                  <p className="text-xs text-gray-500">{order.currency}</p>
                </div>
                <FiChevronRight className="text-gray-400" />
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
