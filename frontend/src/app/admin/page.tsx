'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { admin } from '@/services/api';
import { Order, User } from '@/types';
import { formatKES, formatDate, statusColor } from '@/lib/utils';
import { FiPackage, FiUsers, FiShoppingCart, FiDollarSign } from 'react-icons/fi';

export default function AdminDashboard() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([admin.listOrders(0, 5), admin.listUsers(0, 5)])
      .then(([o, u]) => { setOrders(o.data?.orders || []); setUsers(u.data?.users || []); })
      .finally(() => setLoading(false));
  }, []);

  const stats = [
    { label: 'Total Orders', value: orders.length, icon: FiPackage, color: 'bg-blue-500' },
    { label: 'Total Users', value: users.length, icon: FiUsers, color: 'bg-green-500' },
  ];

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-8">Admin Dashboard</h1>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {stats.map(s => (
          <div key={s.label} className="bg-white rounded-lg p-6 border flex items-center gap-4">
            <div className={`${s.color} p-3 rounded-lg text-white`}><s.icon className="h-6 w-6" /></div>
            <div><p className="text-2xl font-bold">{s.value}</p><p className="text-sm text-gray-500">{s.label}</p></div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <div className="bg-white rounded-lg border">
          <div className="p-4 border-b flex justify-between items-center">
            <h2 className="font-bold">Recent Orders</h2>
            <Link href="/admin/orders" className="text-sm text-primary-600 hover:underline">View All</Link>
          </div>
          <div className="divide-y">
            {loading ? [...Array(3)].map((_, i) => <div key={i} className="h-16 animate-pulse bg-gray-50" />) :
              orders.map(o => (
                <div key={o.id} className="px-4 py-3 flex justify-between items-center">
                  <div>
                    <p className="font-medium text-sm">{o.number}</p>
                    <p className="text-xs text-gray-500">{formatDate(o.createdAt)}</p>
                  </div>
                  <div className="text-right">
                    <span className={`text-xs px-2 py-1 rounded-full ${statusColor(o.status)}`}>{o.status}</span>
                    <p className="text-sm font-medium mt-1">{formatKES(o.total)}</p>
                  </div>
                </div>
              ))}
          </div>
        </div>

        <div className="bg-white rounded-lg border">
          <div className="p-4 border-b flex justify-between items-center">
            <h2 className="font-bold">Recent Users</h2>
            <Link href="/admin/users" className="text-sm text-primary-600 hover:underline">View All</Link>
          </div>
          <div className="divide-y">
            {loading ? [...Array(3)].map((_, i) => <div key={i} className="h-16 animate-pulse bg-gray-50" />) :
              users.map(u => (
                <div key={u.id} className="px-4 py-3 flex justify-between items-center">
                  <div className="flex items-center gap-3">
                    <div className="h-8 w-8 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 text-sm font-medium">{u.name?.charAt(0)}</div>
                    <div><p className="font-medium text-sm">{u.name}</p><p className="text-xs text-gray-500">{u.email}</p></div>
                  </div>
                  <div className="text-right">
                    <p className="text-xs text-gray-500">{u.roles?.join(', ')}</p>
                    <p className="text-xs text-gray-400">{formatDate(u.createdAt || '')}</p>
                  </div>
                </div>
              ))}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-8">
        <Link href="/admin/products" className="bg-white rounded-lg p-6 border hover:shadow-md transition text-center">
          <FiPackage className="h-8 w-8 mx-auto text-primary-600 mb-2" />
          <span className="font-medium">Products</span>
        </Link>
        <Link href="/admin/categories" className="bg-white rounded-lg p-6 border hover:shadow-md transition text-center">
          <FiShoppingCart className="h-8 w-8 mx-auto text-primary-600 mb-2" />
          <span className="font-medium">Categories</span>
        </Link>
        <Link href="/admin/brands" className="bg-white rounded-lg p-6 border hover:shadow-md transition text-center">
          <FiDollarSign className="h-8 w-8 mx-auto text-primary-600 mb-2" />
          <span className="font-medium">Brands</span>
        </Link>
        <Link href="/admin/orders" className="bg-white rounded-lg p-6 border hover:shadow-md transition text-center">
          <FiPackage className="h-8 w-8 mx-auto text-primary-600 mb-2" />
          <span className="font-medium">Orders</span>
        </Link>
      </div>
    </div>
  );
}
